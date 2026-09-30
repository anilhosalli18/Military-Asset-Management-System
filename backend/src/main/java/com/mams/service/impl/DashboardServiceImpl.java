package com.mams.service.impl;

import com.mams.dto.dashboard.*;
import com.mams.model.Purchase;
import com.mams.model.Transfer;
import com.mams.model.enums.AssignmentStatus;
import com.mams.model.enums.TransferStatus;
import com.mams.repository.AssignmentExpenditureRepository;
import com.mams.repository.PurchaseRepository;
import com.mams.repository.TransferRepository;
import com.mams.security.BaseScopeResolver;
import com.mams.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final PurchaseRepository purchaseRepository;
    private final TransferRepository transferRepository;
    private final AssignmentExpenditureRepository assignmentExpenditureRepository;
    private final BaseScopeResolver baseScopeResolver;

    public DashboardServiceImpl(PurchaseRepository purchaseRepository,
                                TransferRepository transferRepository,
                                AssignmentExpenditureRepository assignmentExpenditureRepository,
                                BaseScopeResolver baseScopeResolver) {
        this.purchaseRepository = purchaseRepository;
        this.transferRepository = transferRepository;
        this.assignmentExpenditureRepository = assignmentExpenditureRepository;
        this.baseScopeResolver = baseScopeResolver;
    }

    @Override
    public DashboardMetricsDTO getMetrics(LocalDate startDate,
                                          LocalDate endDate,
                                          Long baseId,
                                          Long equipmentTypeId,
                                          Authentication auth) {
        // Step a: Resolve effective base scope per RBAC
        Long effectiveBaseId = baseScopeResolver.resolveEffectiveBaseId(auth, baseId);

        // Step b: In-range sums [startDate, endDate]
        long purchasesIn = purchaseRepository.sumPurchasesInRange(startDate, endDate, effectiveBaseId, equipmentTypeId);
        long transfersIn = transferRepository.sumTransfersInInRange(startDate, endDate, effectiveBaseId, equipmentTypeId, TransferStatus.completed);
        long transfersOut = transferRepository.sumTransfersOutInRange(startDate, endDate, effectiveBaseId, equipmentTypeId, TransferStatus.completed);
        long expended = assignmentExpenditureRepository.sumExpendedInRange(startDate, endDate, effectiveBaseId, equipmentTypeId, AssignmentStatus.expended);

        // Step c: Pre-range sums (date < startDate)
        long prePurchases = purchaseRepository.sumPurchasesBeforeDate(startDate, effectiveBaseId, equipmentTypeId);
        long preTransfersIn = transferRepository.sumTransfersInBeforeDate(startDate, effectiveBaseId, equipmentTypeId, TransferStatus.completed);
        long preTransfersOut = transferRepository.sumTransfersOutBeforeDate(startDate, effectiveBaseId, equipmentTypeId, TransferStatus.completed);
        long preExpended = assignmentExpenditureRepository.sumExpendedBeforeDate(startDate, effectiveBaseId, equipmentTypeId, AssignmentStatus.expended);

        // Opening balance calculation: sum over everything dated strictly before start_date
        long openingBalance = prePurchases + preTransfersIn - preTransfersOut - preExpended;

        // Step d: Point-in-time currently assigned snapshot (no date filter)
        long assigned = assignmentExpenditureRepository.sumCurrentlyAssigned(effectiveBaseId, equipmentTypeId, AssignmentStatus.assigned);

        // Step e: Compute derived movements and closing balance
        long netMovement = purchasesIn + transfersIn - transfersOut;
        long closingBalance = openingBalance + netMovement - expended;

        // Step f: Assemble DTO
        DashboardFilterDTO filters = DashboardFilterDTO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .baseId(effectiveBaseId)
                .equipmentTypeId(equipmentTypeId)
                .build();

        return DashboardMetricsDTO.builder()
                .filters(filters)
                .openingBalance(openingBalance)
                .closingBalance(closingBalance)
                .netMovement(netMovement)
                .purchases(purchasesIn)
                .transfersIn(transfersIn)
                .transfersOut(transfersOut)
                .assigned(assigned)
                .expended(expended)
                .build();
    }

    @Override
    public NetMovementDetailDTO getNetMovementDetail(LocalDate startDate,
                                                     LocalDate endDate,
                                                     Long baseId,
                                                     Long equipmentTypeId,
                                                     Authentication auth) {
        // Step a: Resolve effective base scope per RBAC
        Long effectiveBaseId = baseScopeResolver.resolveEffectiveBaseId(auth, baseId);

        // Query detailed line items
        List<Purchase> purchases = purchaseRepository.findPurchasesDetail(startDate, endDate, effectiveBaseId, equipmentTypeId);
        List<Transfer> transfersIn = transferRepository.findTransfersInDetail(startDate, endDate, effectiveBaseId, equipmentTypeId, TransferStatus.completed);
        List<Transfer> transfersOut = transferRepository.findTransfersOutDetail(startDate, endDate, effectiveBaseId, equipmentTypeId, TransferStatus.completed);

        List<PurchaseLineItemDTO> purchaseLineItems = purchases.stream()
                .map(p -> PurchaseLineItemDTO.builder()
                        .id(p.getId())
                        .purchaseDate(p.getPurchaseDate())
                        .quantity(p.getQuantity())
                        .vendor(p.getVendor())
                        .baseName(p.getBase() != null ? p.getBase().getName() : null)
                        .equipmentName(p.getEquipmentType() != null ? p.getEquipmentType().getName() : null)
                        .build())
                .toList();

        List<TransferLineItemDTO> transfersInLineItems = transfersIn.stream()
                .map(t -> TransferLineItemDTO.builder()
                        .id(t.getId())
                        .transferDate(t.getTransferDate())
                        .quantity(t.getQuantity())
                        .fromBaseName(t.getFromBase() != null ? t.getFromBase().getName() : null)
                        .toBaseName(t.getToBase() != null ? t.getToBase().getName() : null)
                        .equipmentName(t.getEquipmentType() != null ? t.getEquipmentType().getName() : null)
                        .build())
                .toList();

        List<TransferLineItemDTO> transfersOutLineItems = transfersOut.stream()
                .map(t -> TransferLineItemDTO.builder()
                        .id(t.getId())
                        .transferDate(t.getTransferDate())
                        .quantity(t.getQuantity())
                        .fromBaseName(t.getFromBase() != null ? t.getFromBase().getName() : null)
                        .toBaseName(t.getToBase() != null ? t.getToBase().getName() : null)
                        .equipmentName(t.getEquipmentType() != null ? t.getEquipmentType().getName() : null)
                        .build())
                .toList();

        return NetMovementDetailDTO.builder()
                .purchases(purchaseLineItems)
                .transfersIn(transfersInLineItems)
                .transfersOut(transfersOutLineItems)
                .build();
    }
}
