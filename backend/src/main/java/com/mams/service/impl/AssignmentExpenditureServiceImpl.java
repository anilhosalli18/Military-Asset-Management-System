package com.mams.service.impl;

import com.mams.dto.assignment.AssignmentDTO;
import com.mams.dto.assignment.CreateAssignmentRequest;
import com.mams.exception.BadRequestException;
import com.mams.exception.ResourceNotFoundException;
import com.mams.exception.UnauthorizedBaseAccessException;
import com.mams.model.AssignmentExpenditure;
import com.mams.model.Base;
import com.mams.model.EquipmentType;
import com.mams.model.User;
import com.mams.model.enums.AssignmentStatus;
import com.mams.model.enums.Role;
import com.mams.repository.AssignmentExpenditureRepository;
import com.mams.repository.BaseRepository;
import com.mams.repository.EquipmentTypeRepository;
import com.mams.repository.UserRepository;
import com.mams.security.BaseScopeResolver;
import com.mams.security.UserPrincipal;
import com.mams.service.AssignmentExpenditureService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class AssignmentExpenditureServiceImpl implements AssignmentExpenditureService {

    private final AssignmentExpenditureRepository assignmentExpenditureRepository;
    private final BaseRepository baseRepository;
    private final EquipmentTypeRepository equipmentTypeRepository;
    private final UserRepository userRepository;
    private final BaseScopeResolver baseScopeResolver;

    public AssignmentExpenditureServiceImpl(AssignmentExpenditureRepository assignmentExpenditureRepository,
                                           BaseRepository baseRepository,
                                           EquipmentTypeRepository equipmentTypeRepository,
                                           UserRepository userRepository,
                                           BaseScopeResolver baseScopeResolver) {
        this.assignmentExpenditureRepository = assignmentExpenditureRepository;
        this.baseRepository = baseRepository;
        this.equipmentTypeRepository = equipmentTypeRepository;
        this.userRepository = userRepository;
        this.baseScopeResolver = baseScopeResolver;
    }

    @Override
    public AssignmentDTO createAssignment(CreateAssignmentRequest request, Authentication auth) {
        UserPrincipal principal = validateCommanderOrAdmin(auth);

        if (request == null) {
            throw new BadRequestException("Assignment request cannot be null");
        }

        // Validate quantity
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        // Validate personnel name
        if (request.getPersonnelName() == null || request.getPersonnelName().trim().isEmpty()) {
            throw new BadRequestException("Personnel name is required");
        }

        // Validate assigned date
        if (request.getAssignedDate() == null) {
            throw new BadRequestException("Assigned date is required");
        }
        if (request.getAssignedDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Assigned date cannot be in the future");
        }

        if (request.getEquipmentTypeId() == null) {
            throw new BadRequestException("Equipment type ID is required");
        }

        // Base-scoping: BASE_COMMANDER forced to own base; ADMIN can specify any base
        Long effectiveBaseId = baseScopeResolver.resolveEffectiveBaseId(auth, request.getBaseId());
        if (effectiveBaseId == null) {
            throw new BadRequestException("Base ID is required");
        }

        Base base = baseRepository.findById(effectiveBaseId)
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Active base not found with ID: " + effectiveBaseId));

        EquipmentType equipmentType = equipmentTypeRepository.findById(request.getEquipmentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment type not found with ID: " + request.getEquipmentTypeId()));

        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + principal.getId()));

        String personnelIdNo = (request.getPersonnelIdNo() != null && !request.getPersonnelIdNo().trim().isEmpty())
                ? request.getPersonnelIdNo().trim()
                : "MIL-" + System.currentTimeMillis() % 100000;

        AssignmentExpenditure assignment = AssignmentExpenditure.builder()
                .base(base)
                .equipmentType(equipmentType)
                .personnelName(request.getPersonnelName().trim())
                .personnelIdNo(personnelIdNo)
                .quantity(request.getQuantity())
                .status(AssignmentStatus.assigned)
                .assignedDate(request.getAssignedDate())
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .createdBy(creator)
                .build();

        AssignmentExpenditure saved = assignmentExpenditureRepository.save(assignment);
        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentDTO> getAssignments(String status,
                                              LocalDate startDate,
                                              LocalDate endDate,
                                              Long baseId,
                                              Long equipmentTypeId,
                                              Pageable pageable,
                                              Authentication auth) {
        validateCommanderOrAdmin(auth);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("start_date cannot be after end_date");
        }

        Long effectiveBaseId = baseScopeResolver.resolveEffectiveBaseId(auth, baseId);

        AssignmentStatus assignmentStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                assignmentStatus = AssignmentStatus.fromValue(status.trim().toLowerCase());
            } catch (Exception ignored) {
                // Keep as null if not matching
            }
        }

        Pageable effectivePageable = pageable;
        if (pageable == null || pageable.getSort().isUnsorted()) {
            int page = pageable != null ? pageable.getPageNumber() : 0;
            int size = pageable != null ? pageable.getPageSize() : 10;
            effectivePageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedDate"));
        }

        Page<AssignmentExpenditure> pageResult = assignmentExpenditureRepository.findAssignmentsPaged(
                effectiveBaseId, equipmentTypeId, assignmentStatus, startDate, endDate, effectivePageable);

        return pageResult.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentDTO getAssignmentById(Long id, Authentication auth) {
        UserPrincipal principal = validateCommanderOrAdmin(auth);

        AssignmentExpenditure assignment = assignmentExpenditureRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment record not found with ID: " + id));

        if (principal.getRole() != Role.ADMIN) {
            Long ownBaseId = principal.getBaseId();
            if (ownBaseId == null || !ownBaseId.equals(assignment.getBase().getId())) {
                throw new UnauthorizedBaseAccessException("Access denied: Assignment record is outside your jurisdiction");
            }
        }

        return mapToDTO(assignment);
    }

    @Override
    public AssignmentDTO markExpended(Long id, LocalDate expendedDate, String notes, Authentication auth) {
        UserPrincipal principal = validateCommanderOrAdmin(auth);

        AssignmentExpenditure assignment = assignmentExpenditureRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment record not found with ID: " + id));

        // Base-scoping check
        if (principal.getRole() != Role.ADMIN) {
            Long ownBaseId = principal.getBaseId();
            if (ownBaseId == null || !ownBaseId.equals(assignment.getBase().getId())) {
                throw new UnauthorizedBaseAccessException("Access denied: Assignment belongs to another base outside your jurisdiction");
            }
        }

        // Only valid from status = 'assigned'
        if (assignment.getStatus() != AssignmentStatus.assigned) {
            throw new BadRequestException("Cannot expend assignment with status: " + assignment.getStatus() +
                    ". Only 'assigned' records may be marked as expended.");
        }

        LocalDate effectiveExpendedDate = (expendedDate != null) ? expendedDate : LocalDate.now();
        if (effectiveExpendedDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("Expended date cannot be in the future");
        }

        assignment.setStatus(AssignmentStatus.expended);
        assignment.setExpendedDate(effectiveExpendedDate);

        if (notes != null && !notes.trim().isEmpty()) {
            String updatedNotes = (assignment.getNotes() != null && !assignment.getNotes().trim().isEmpty())
                    ? assignment.getNotes() + "\n[Expended]: " + notes.trim()
                    : "[Expended]: " + notes.trim();
            assignment.setNotes(updatedNotes);
        }

        AssignmentExpenditure updated = assignmentExpenditureRepository.save(assignment);
        return mapToDTO(updated);
    }

    @Override
    public AssignmentDTO markReturned(Long id, LocalDate returnedDate, String notes, Authentication auth) {
        UserPrincipal principal = validateCommanderOrAdmin(auth);

        AssignmentExpenditure assignment = assignmentExpenditureRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment record not found with ID: " + id));

        // Base-scoping check
        if (principal.getRole() != Role.ADMIN) {
            Long ownBaseId = principal.getBaseId();
            if (ownBaseId == null || !ownBaseId.equals(assignment.getBase().getId())) {
                throw new UnauthorizedBaseAccessException("Access denied: Assignment belongs to another base outside your jurisdiction");
            }
        }

        // Only valid from status = 'assigned'
        if (assignment.getStatus() != AssignmentStatus.assigned) {
            throw new BadRequestException("Cannot return assignment with status: " + assignment.getStatus() +
                    ". Only 'assigned' records may be marked as returned.");
        }

        LocalDate effectiveReturnedDate = (returnedDate != null) ? returnedDate : LocalDate.now();
        if (effectiveReturnedDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("Returned date cannot be in the future");
        }

        assignment.setStatus(AssignmentStatus.returned);
        assignment.setReturnedDate(effectiveReturnedDate);

        if (notes != null && !notes.trim().isEmpty()) {
            String updatedNotes = (assignment.getNotes() != null && !assignment.getNotes().trim().isEmpty())
                    ? assignment.getNotes() + "\n[Returned]: " + notes.trim()
                    : "[Returned]: " + notes.trim();
            assignment.setNotes(updatedNotes);
        }

        AssignmentExpenditure updated = assignmentExpenditureRepository.save(assignment);
        return mapToDTO(updated);
    }

    private UserPrincipal validateCommanderOrAdmin(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadRequestException("Authenticated user credentials required");
        }
        if (principal.getRole() == Role.LOGISTICS_OFFICER) {
            throw new UnauthorizedBaseAccessException("Access denied: Logistics Officers are not permitted to manage asset assignments and expenditures");
        }
        if (principal.getRole() != Role.ADMIN && principal.getRole() != Role.BASE_COMMANDER) {
            throw new UnauthorizedBaseAccessException("Access denied: You do not have permission to manage assignments");
        }
        return principal;
    }

    private AssignmentDTO mapToDTO(AssignmentExpenditure a) {
        return AssignmentDTO.builder()
                .id(a.getId())
                .baseId(a.getBase() != null ? a.getBase().getId() : null)
                .baseName(a.getBase() != null ? a.getBase().getName() : null)
                .equipmentTypeId(a.getEquipmentType() != null ? a.getEquipmentType().getId() : null)
                .equipmentName(a.getEquipmentType() != null ? a.getEquipmentType().getName() : null)
                .personnelName(a.getPersonnelName())
                .personnelIdNo(a.getPersonnelIdNo())
                .quantity(a.getQuantity())
                .status(a.getStatus())
                .assignedDate(a.getAssignedDate())
                .expendedDate(a.getExpendedDate())
                .returnedDate(a.getReturnedDate())
                .notes(a.getNotes())
                .createdBy(a.getCreatedBy() != null ? a.getCreatedBy().getId() : null)
                .createdByUsername(a.getCreatedBy() != null ? a.getCreatedBy().getUsername() : null)
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
