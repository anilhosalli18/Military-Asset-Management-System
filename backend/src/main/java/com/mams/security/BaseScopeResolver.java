package com.mams.security;

import com.mams.model.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class BaseScopeResolver {

    /**
     * Resolves the effective base ID based on the authenticated user's role:
     * - ADMIN: Can view all bases (null) or scope to a specific requested baseId.
     * - BASE_COMMANDER & LOGISTICS_OFFICER: Forcibly scoped strictly to their assigned baseId
     *   from their authentication credentials, ignoring/overriding any client-requested baseId.
     */
    public Long resolveEffectiveBaseId(Authentication authentication, Long requestedBaseId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalArgumentException("Valid user authentication is required to resolve base scope");
        }

        if (principal.getRole() == Role.ADMIN) {
            return requestedBaseId;
        }

        // For non-admin roles (BASE_COMMANDER, LOGISTICS_OFFICER), enforce their own assigned baseId
        return principal.getBaseId();
    }
}
