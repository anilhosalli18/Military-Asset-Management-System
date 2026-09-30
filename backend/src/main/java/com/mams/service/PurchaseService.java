package com.mams.service;

import com.mams.dto.purchase.CreatePurchaseRequest;
import com.mams.dto.purchase.PurchaseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;

public interface PurchaseService {
    PurchaseDTO createPurchase(CreatePurchaseRequest request, Authentication auth);
    Page<PurchaseDTO> getPurchases(LocalDate startDate, LocalDate endDate, Long baseId, Long equipmentTypeId, Pageable pageable, Authentication auth);
    PurchaseDTO getPurchaseById(Long id, Authentication auth);
}
