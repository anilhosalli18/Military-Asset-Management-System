package com.mams.controller;

import com.mams.dto.common.ApiResponse;
import com.mams.dto.purchase.CreatePurchaseRequest;
import com.mams.dto.purchase.PurchaseDTO;
import com.mams.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller for managing purchases.
 * NOTE: PUT and DELETE endpoints are intentionally omitted to preserve financial ledger immutability
 * and audit integrity. Any corrections must be performed as separate reversing entries.
 */
@RestController
@RequestMapping("/api/purchases")
@Tag(name = "Purchases", description = "Endpoints for managing asset purchases")
@SecurityRequirement(name = "BearerAuth")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BASE_COMMANDER', 'LOGISTICS_OFFICER')")
    @Operation(summary = "Record a new asset purchase")
    public ResponseEntity<ApiResponse<PurchaseDTO>> createPurchase(
            @Valid @RequestBody CreatePurchaseRequest request,
            Authentication authentication) {
        PurchaseDTO created = purchaseService.createPurchase(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BASE_COMMANDER', 'LOGISTICS_OFFICER')")
    @Operation(summary = "Get paginated list of purchases with optional filters")
    public ResponseEntity<ApiResponse<Page<PurchaseDTO>>> getPurchases(
            @RequestParam(value = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateParam,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateCamel,
            @RequestParam(value = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateParam,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateCamel,
            @RequestParam(value = "base_id", required = false) Long baseIdParam,
            @RequestParam(value = "baseId", required = false) Long baseIdCamel,
            @RequestParam(value = "equipment_type_id", required = false) Long equipmentTypeIdParam,
            @RequestParam(value = "equipmentTypeId", required = false) Long equipmentTypeIdCamel,
            @PageableDefault(size = 10, sort = "purchaseDate") Pageable pageable,
            Authentication authentication) {

        LocalDate startDate = startDateParam != null ? startDateParam : startDateCamel;
        LocalDate endDate = endDateParam != null ? endDateParam : endDateCamel;
        Long baseId = baseIdParam != null ? baseIdParam : baseIdCamel;
        Long equipmentTypeId = equipmentTypeIdParam != null ? equipmentTypeIdParam : equipmentTypeIdCamel;

        Page<PurchaseDTO> purchases = purchaseService.getPurchases(startDate, endDate, baseId, equipmentTypeId, pageable, authentication);
        return ResponseEntity.ok(ApiResponse.of(purchases));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BASE_COMMANDER', 'LOGISTICS_OFFICER')")
    @Operation(summary = "Get purchase details by ID")
    public ResponseEntity<ApiResponse<PurchaseDTO>> getPurchaseById(
            @PathVariable Long id,
            Authentication authentication) {
        PurchaseDTO purchase = purchaseService.getPurchaseById(id, authentication);
        return ResponseEntity.ok(ApiResponse.of(purchase));
    }
}
