package com.mams.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mams.model.AuditLog;
import com.mams.model.User;
import com.mams.repository.AuditLogRepository;
import com.mams.repository.UserRepository;
import com.mams.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class AuditLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditLoggingAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AuditLoggingAspect(AuditLogRepository auditLogRepository,
                              UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Pointcut("within(com.mams.controller..*) && (" +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public void mutatingControllerMethods() {}

    @Around("mutatingControllerMethods()")
    public Object logMutatingAction(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        int statusCode = 200;
        Throwable error = null;

        try {
            result = joinPoint.proceed();
            if (result instanceof ResponseEntity<?> responseEntity) {
                statusCode = responseEntity.getStatusCode().value();
            }
            return result;
        } catch (Throwable ex) {
            error = ex;
            statusCode = 500;
            throw ex;
        } finally {
            try {
                recordAuditLog(joinPoint, statusCode, error, result);
            } catch (Exception ex) {
                // Must not block the response on log failure
                logger.error("Failed to write audit log asynchronously/safely: {}", ex.getMessage(), ex);
            }
        }
    }

    private void recordAuditLog(ProceedingJoinPoint joinPoint, int statusCode, Throwable error, Object result) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        String method = request.getMethod();
        String endpoint = request.getRequestURI();
        String ipAddress = request.getRemoteAddr();

        // Extract logged-in user
        User user = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            user = userRepository.findById(principal.getId()).orElse(null);
        }

        // Determine action & entity type
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName().replace("Controller", "");
        String action = method + " " + methodName;
        String entityType = className.toUpperCase();
        String entityId = null;

        // Custom mapping for purchases & transfers
        if ("PurchaseController".equalsIgnoreCase(joinPoint.getTarget().getClass().getSimpleName())
                || endpoint.contains("/purchases")) {
            entityType = "purchases";
            if ("POST".equalsIgnoreCase(method)) {
                action = "CREATE_PURCHASE";
            }
        } else if ("TransferController".equalsIgnoreCase(joinPoint.getTarget().getClass().getSimpleName())
                || endpoint.contains("/transfers")) {
            entityType = "transfers";
            if ("POST".equalsIgnoreCase(method)) {
                action = "CREATE_TRANSFER";
            } else if ("PATCH".equalsIgnoreCase(method)) {
                action = "UPDATE_TRANSFER_STATUS";
            }
        } else if ("AssignmentExpenditureController".equalsIgnoreCase(joinPoint.getTarget().getClass().getSimpleName())
                || endpoint.contains("/assignments")) {
            entityType = "assignments";
            if ("POST".equalsIgnoreCase(method)) {
                action = "CREATE_ASSIGNMENT";
            } else if ("PATCH".equalsIgnoreCase(method)) {
                if (endpoint.contains("/expend")) {
                    action = "MARK_EXPENDED";
                } else if (endpoint.contains("/return")) {
                    action = "MARK_RETURNED";
                } else {
                    action = "UPDATE_ASSIGNMENT_STATUS";
                }
            }
        } else if ("UserController".equalsIgnoreCase(joinPoint.getTarget().getClass().getSimpleName())
                || endpoint.contains("/users")) {
            entityType = "users";
            if ("POST".equalsIgnoreCase(method)) {
                action = "CREATE_USER";
            } else if ("PUT".equalsIgnoreCase(method)) {
                action = "UPDATE_USER";
            } else if ("PATCH".equalsIgnoreCase(method)) {
                if (endpoint.contains("/deactivate")) {
                    action = "DEACTIVATE_USER";
                } else if (endpoint.contains("/reset-password")) {
                    action = "RESET_PASSWORD";
                } else {
                    action = "UPDATE_USER";
                }
            }
        }

        // Extract entity_id from result (e.g. data.id)
        if (result instanceof ResponseEntity<?> responseEntity && responseEntity.getBody() != null) {
            entityId = extractEntityIdFromBody(responseEntity.getBody());
        }

        // Build details JSON
        Map<String, Object> details = new HashMap<>();
        details.put("args", sanitizeArgs(joinPoint.getArgs()));
        if (error != null) {
            details.put("error", error.getMessage());
        }

        String detailsJson = null;
        try {
            detailsJson = objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            logger.warn("Could not serialize audit log details to JSON: {}", e.getMessage());
        }

        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .method(method)
                .endpoint(endpoint)
                .statusCode(statusCode)
                .detailsJson(detailsJson)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(log);
    }

    private String extractEntityIdFromBody(Object body) {
        if (body == null) return null;
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.valueToTree(body);
            if (node.hasNonNull("id")) {
                return node.get("id").asText();
            }
            if (node.hasNonNull("data") && node.get("data").hasNonNull("id")) {
                return node.get("data").get("id").asText();
            }
        } catch (Exception e) {
            logger.debug("Could not extract entity_id from response body: {}", e.getMessage());
        }
        return null;
    }

    private Object[] sanitizeArgs(Object[] args) {
        if (args == null) return new Object[0];
        Object[] sanitized = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg != null && (arg.getClass().getName().toLowerCase().contains("login")
                    || arg.getClass().getName().toLowerCase().contains("password")
                    || arg.getClass().getName().toLowerCase().contains("createuser"))) {
                sanitized[i] = "[PROTECTED_CREDENTIALS]";
            } else {
                sanitized[i] = arg;
            }
        }
        return sanitized;
    }
}
