package com.mams.service.impl;

import com.mams.dto.purchase.CreatePurchaseRequest;
import com.mams.dto.purchase.PurchaseDTO;
import com.mams.exception.BadRequestException;
import com.mams.exception.ResourceNotFoundException;
import com.mams.exception.UnauthorizedBaseAccessException;
import com.mams.model.Base;
import com.mams.model.EquipmentType;
import com.mams.model.Purchase;
import com.mams.model.User;
import com.mams.model.enums.Role;
import com.mams.repository.BaseRepository;
import com.mams.repository.EquipmentTypeRepository;
import com.mams.repository.PurchaseRepository;
import com.mams.repository.UserRepository;
import com.mams.security.BaseScopeResolver;
import com.mams.security.UserPrincipal;
import com.mams.service.PurchaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Transactional
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final BaseRepository baseRepository;
    private final EquipmentTypeRepository equipmentTypeRepository;
    private final UserRepository userRepository;
    private final BaseScopeResolver baseScopeResolver;

    public PurchaseServiceImpl(PurchaseRepository purchaseRepository,
                               BaseRepository baseRepository,
                               EquipmentTypeRepository equipmentTypeRepository,
                               UserRepository userRepository,
                               BaseScopeResolver baseScopeResolver) {
        this.purchaseRepository = purchaseRepository;
        this.baseRepository = baseRepository;
        this.equipmentTypeRepository = equipmentTypeRepository;
        this.userRepository = userRepository;
        this.baseScopeResolver = baseScopeResolver;
    }

    @Override
    public PurchaseDTO createPurchase(CreatePurchaseRequest request, Authentication auth) {
        if (request == null) {
            throw new BadRequestException("Purchase request cannot be null");
        }

        // Validate quantity
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        // Validate purchase date
        if (request.getPurchaseDate() == null) {
            throw new BadRequestException("Purchase date is required");
        }
        if (request.getPurchaseDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Purchase date cannot be in the future");
        }

        // Base scoping resolution: non-admins are forced to their own base;
        // ADMIN can record for any specified base
        Long effectiveBaseId = baseScopeResolver.resolveEffectiveBaseId(auth, request.getBaseId());
        if (effectiveBaseId == null) {
            throw new BadRequestException("Base ID is required");
        }

        if (request.getEquipmentTypeId() == null) {
            throw new BadRequestException("Equipment type ID is required");
        }

        // Validate active Base exists (404 if not found or inactive)
        Base base = baseRepository.findById(effectiveBaseId)
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Active base not found with ID: " + effectiveBaseId));

        // Validate EquipmentType exists (404 if not found)
        EquipmentType equipmentType = equipmentTypeRepository.findById(request.getEquipmentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment type not found with ID: " + request.getEquipmentTypeId()));

        // Resolve authenticated user as creator
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadRequestException("Authenticated user credentials required");
        }
        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + principal.getId()));

        // Calculate costs
        BigDecimal unitCost = request.getUnitCost() != null ? request.getUnitCost() : BigDecimal.ZERO;
        BigDecimal totalCost = unitCost.multiply(BigDecimal.valueOf(request.getQuantity()));

        String vendor = (request.getVendor() != null && !request.getVendor().trim().isEmpty())
                ? request.getVendor().trim()
                : "Standard Issue / Government Contractor";

        Purchase purchase = Purchase.builder()
                .base(base)
                .equipmentType(equipmentType)
                .quantity(request.getQuantity())
                .unitCost(unitCost)
                .totalCost(totalCost)
                .vendor(vendor)
                .purchaseDate(request.getPurchaseDate())
                .notes(request.getNotes())
                .createdBy(creator)
                .build();

        Purchase saved = purchaseRepository.save(purchase);
        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseDTO> getPurchases(LocalDate startDate,
                                          LocalDate endDate,
                                          Long baseId,
                                          Long equipmentTypeId,
                                          Pageable pageable,
                                          Authentication auth) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("start_date cannot be after end_date");
        }

        // Non-admins are forcibly scoped to their own base regardless of baseId param
        Long effectiveBaseId = baseScopeResolver.resolveEffectiveBaseId(auth, baseId);

        // Sort by purchaseDate DESC by default if unsorted
        Pageable effectivePageable = pageable;
        if (pageable == null || pageable.getSort().isUnsorted()) {
            int page = pageable != null ? pageable.getPageNumber() : 0;
            int size = pageable != null ? pageable.getPageSize() : 10;
            effectivePageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "purchaseDate"));
        }

        Page<Purchase> purchases = purchaseRepository.findFilteredPaged(
                effectiveBaseId, equipmentTypeId, startDate, endDate, effectivePageable);

        return purchases.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseDTO getPurchaseById(Long id, Authentication auth) {
        Purchase purchase = purchaseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with ID: " + id));

        // 403 if it belongs to a base outside the caller's scope (for non-admins)
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            if (principal.getRole() != Role.ADMIN) {
                if (principal.getBaseId() == null || !principal.getBaseId().equals(purchase.getBase().getId())) {
                    throw new UnauthorizedBaseAccessException("Access denied: Purchase belongs to another base outside your jurisdiction");
                }
            }
        }

        return mapToDTO(purchase);
    }

    private PurchaseDTO mapToDTO(Purchase p) {
        return PurchaseDTO.builder()
                .id(p.getId())
                .baseId(p.getBase() != null ? p.getBase().getId() : null)
                .baseName(p.getBase() != null ? p.getBase().getName() : null)
                .equipmentTypeId(p.getEquipmentType() != null ? p.getEquipmentType().getId() : null)
                .equipmentName(p.getEquipmentType() != null ? p.getEquipmentType().getName() : null)
                .quantity(p.getQuantity())
                .unitCost(p.getUnitCost())
                .totalCost(p.getTotalCost())
                .vendor(p.getVendor())
                .purchaseDate(p.getPurchaseDate())
                .notes(p.getNotes())
                .createdBy(p.getCreatedBy() != null ? p.getCreatedBy().getId() : null)
                .createdByUsername(p.getCreatedBy() != null ? p.getCreatedBy().getUsername() : null)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
