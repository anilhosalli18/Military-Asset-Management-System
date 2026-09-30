package com.mams.service.impl;

import com.mams.dto.transfer.CreateTransferRequest;
import com.mams.dto.transfer.TransferDTO;
import com.mams.exception.BadRequestException;
import com.mams.exception.ResourceNotFoundException;
import com.mams.exception.UnauthorizedBaseAccessException;
import com.mams.model.Base;
import com.mams.model.EquipmentType;
import com.mams.model.Transfer;
import com.mams.model.User;
import com.mams.model.enums.Role;
import com.mams.model.enums.TransferStatus;
import com.mams.repository.BaseRepository;
import com.mams.repository.EquipmentTypeRepository;
import com.mams.repository.TransferRepository;
import com.mams.repository.UserRepository;
import com.mams.security.UserPrincipal;
import com.mams.service.TransferService;
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
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final BaseRepository baseRepository;
    private final EquipmentTypeRepository equipmentTypeRepository;
    private final UserRepository userRepository;

    public TransferServiceImpl(TransferRepository transferRepository,
                               BaseRepository baseRepository,
                               EquipmentTypeRepository equipmentTypeRepository,
                               UserRepository userRepository) {
        this.transferRepository = transferRepository;
        this.baseRepository = baseRepository;
        this.equipmentTypeRepository = equipmentTypeRepository;
        this.userRepository = userRepository;
    }

    @Override
    public TransferDTO createTransfer(CreateTransferRequest request, Authentication auth) {
        if (request == null) {
            throw new BadRequestException("Transfer request cannot be null");
        }

        // Validate quantity
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        // Validate from_base != to_base
        if (request.getFromBaseId() == null || request.getToBaseId() == null) {
            throw new BadRequestException("Both source and destination bases are required");
        }
        if (request.getFromBaseId().equals(request.getToBaseId())) {
            throw new BadRequestException("Source and destination bases cannot be identical");
        }

        // Validate transfer date
        if (request.getTransferDate() == null) {
            throw new BadRequestException("Transfer date is required");
        }
        if (request.getTransferDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Transfer date cannot be in the future");
        }

        if (request.getEquipmentTypeId() == null) {
            throw new BadRequestException("Equipment type ID is required");
        }

        // Authenticated user resolution
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadRequestException("Authenticated user credentials required");
        }

        // RBAC: ADMIN can transfer between any bases. Non-admins must belong to either from or to base.
        if (principal.getRole() != Role.ADMIN) {
            Long callerBaseId = principal.getBaseId();
            if (callerBaseId == null ||
                (!callerBaseId.equals(request.getFromBaseId()) && !callerBaseId.equals(request.getToBaseId()))) {
                throw new UnauthorizedBaseAccessException(
                    "Access denied: You may only initiate transfers where your base is either the sender or receiver"
                );
            }
        }

        // Validate existing active rows
        Base fromBase = baseRepository.findById(request.getFromBaseId())
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Active source base not found with ID: " + request.getFromBaseId()));

        Base toBase = baseRepository.findById(request.getToBaseId())
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Active destination base not found with ID: " + request.getToBaseId()));

        EquipmentType equipmentType = equipmentTypeRepository.findById(request.getEquipmentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment type not found with ID: " + request.getEquipmentTypeId()));

        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + principal.getId()));

        // Default status to 'completed' unless explicitly specified as 'pending' or 'in_transit'
        TransferStatus status = request.getStatus() != null ? request.getStatus() : TransferStatus.completed;

        Transfer transfer = Transfer.builder()
                .equipmentType(equipmentType)
                .fromBase(fromBase)
                .toBase(toBase)
                .quantity(request.getQuantity())
                .transferDate(request.getTransferDate())
                .status(status)
                .notes(request.getNotes())
                .createdBy(creator)
                .build();

        Transfer saved = transferRepository.save(transfer);
        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransferDTO> getTransfers(LocalDate startDate,
                                          LocalDate endDate,
                                          Long baseId,
                                          Long equipmentTypeId,
                                          String direction,
                                          Pageable pageable,
                                          Authentication auth) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("start_date cannot be after end_date");
        }

        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadRequestException("Authenticated user credentials required");
        }

        // Base scoping: for non-admins, own base is always enforced
        Long effectiveBaseId;
        if (principal.getRole() == Role.ADMIN) {
            effectiveBaseId = baseId;
        } else {
            effectiveBaseId = principal.getBaseId();
        }

        String normalizedDir = null;
        if (direction != null && !direction.trim().isEmpty()) {
            String d = direction.trim().toLowerCase();
            if ("in".equals(d) || "out".equals(d)) {
                normalizedDir = d;
            }
        }

        Pageable effectivePageable = pageable;
        if (pageable == null || pageable.getSort().isUnsorted()) {
            int page = pageable != null ? pageable.getPageNumber() : 0;
            int size = pageable != null ? pageable.getPageSize() : 10;
            effectivePageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transferDate"));
        }

        Page<Transfer> pageResult = transferRepository.findTransfersPaged(
                effectiveBaseId, normalizedDir, equipmentTypeId, startDate, endDate, effectivePageable);

        return pageResult.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferDTO getTransferById(Long id, Authentication auth) {
        Transfer transfer = transferRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found with ID: " + id));

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            if (principal.getRole() != Role.ADMIN) {
                Long ownBaseId = principal.getBaseId();
                if (ownBaseId == null ||
                    (!ownBaseId.equals(transfer.getFromBase().getId()) && !ownBaseId.equals(transfer.getToBase().getId()))) {
                    throw new UnauthorizedBaseAccessException("Access denied: Transfer is outside your assigned base jurisdiction");
                }
            }
        }

        return mapToDTO(transfer);
    }

    @Override
    public TransferDTO updateTransferStatus(Long id, TransferStatus newStatus, Authentication auth) {
        if (newStatus == null) {
            throw new BadRequestException("New transfer status is required");
        }

        Transfer transfer = transferRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found with ID: " + id));

        // Same base-scoping rule as create: caller must be ADMIN or have from/to base matching their own
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            if (principal.getRole() != Role.ADMIN) {
                Long ownBaseId = principal.getBaseId();
                if (ownBaseId == null ||
                    (!ownBaseId.equals(transfer.getFromBase().getId()) && !ownBaseId.equals(transfer.getToBase().getId()))) {
                    throw new UnauthorizedBaseAccessException("Access denied: You may only update transfers involving your assigned base");
                }
            }
        }

        TransferStatus currentStatus = transfer.getStatus();
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        transfer.setStatus(newStatus);
        Transfer updated = transferRepository.save(transfer);
        return mapToDTO(updated);
    }

    /**
     * State machine validation:
     * pending -> in_transit, completed, cancelled
     * in_transit -> completed, cancelled
     * any status -> cancelled
     * completed -> cannot move back to pending or in_transit
     */
    private boolean isValidTransition(TransferStatus current, TransferStatus next) {
        if (current == next) return true;
        if (next == TransferStatus.cancelled) return true;
        if (current == TransferStatus.pending) {
            return next == TransferStatus.in_transit || next == TransferStatus.completed;
        }
        if (current == TransferStatus.in_transit) {
            return next == TransferStatus.completed;
        }
        return false;
    }

    private TransferDTO mapToDTO(Transfer t) {
        return TransferDTO.builder()
                .id(t.getId())
                .equipmentTypeId(t.getEquipmentType() != null ? t.getEquipmentType().getId() : null)
                .equipmentName(t.getEquipmentType() != null ? t.getEquipmentType().getName() : null)
                .fromBaseId(t.getFromBase() != null ? t.getFromBase().getId() : null)
                .fromBaseName(t.getFromBase() != null ? t.getFromBase().getName() : null)
                .toBaseId(t.getToBase() != null ? t.getToBase().getId() : null)
                .toBaseName(t.getToBase() != null ? t.getToBase().getName() : null)
                .quantity(t.getQuantity())
                .transferDate(t.getTransferDate())
                .status(t.getStatus())
                .notes(t.getNotes())
                .createdBy(t.getCreatedBy() != null ? t.getCreatedBy().getId() : null)
                .createdByUsername(t.getCreatedBy() != null ? t.getCreatedBy().getUsername() : null)
                .createdAt(t.getCreatedAt())
                .build();
    }
}
