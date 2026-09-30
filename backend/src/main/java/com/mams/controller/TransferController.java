package com.mams.controller;

import com.mams.dto.common.ApiResponse;
import com.mams.dto.transfer.CreateTransferRequest;
import com.mams.dto.transfer.TransferDTO;
import com.mams.dto.transfer.UpdateTransferStatusRequest;
import com.mams.model.enums.TransferStatus;
import com.mams.service.TransferService;
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

@RestController
@RequestMapping("/api/transfers")
@Tag(name = "Transfers", description = "Endpoints for managing inter-base asset transfers")
@SecurityRequirement(name = "BearerAuth")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BASE_COMMANDER', 'LOGISTICS_OFFICER')")
    @Operation(summary = "Initiate an asset transfer between bases")
    public ResponseEntity<ApiResponse<TransferDTO>> createTransfer(
            @Valid @RequestBody CreateTransferRequest request,
            Authentication authentication) {
        TransferDTO created = transferService.createTransfer(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BASE_COMMANDER', 'LOGISTICS_OFFICER')")
    @Operation(summary = "Get paginated history of asset transfers with optional filters")
    public ResponseEntity<ApiResponse<Page<TransferDTO>>> getTransfers(
            @RequestParam(value = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateParam,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateCamel,
            @RequestParam(value = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateParam,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateCamel,
            @RequestParam(value = "base_id", required = false) Long baseIdParam,
            @RequestParam(value = "baseId", required = false) Long baseIdCamel,
            @RequestParam(value = "equipment_type_id", required = false) Long equipmentTypeIdParam,
            @RequestParam(value = "equipmentTypeId", required = false) Long equipmentTypeIdCamel,
            @RequestParam(value = "direction", required = false) String direction,
            @PageableDefault(size = 10, sort = "transferDate") Pageable pageable,
            Authentication authentication) {

        LocalDate startDate = startDateParam != null ? startDateParam : startDateCamel;
        LocalDate endDate = endDateParam != null ? endDateParam : endDateCamel;
        Long baseId = baseIdParam != null ? baseIdParam : baseIdCamel;
        Long equipmentTypeId = equipmentTypeIdParam != null ? equipmentTypeIdParam : equipmentTypeIdCamel;

        Page<TransferDTO> transfers = transferService.getTransfers(
                startDate, endDate, baseId, equipmentTypeId, direction, pageable, authentication);
        return ResponseEntity.ok(ApiResponse.of(transfers));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BASE_COMMANDER', 'LOGISTICS_OFFICER')")
    @Operation(summary = "Get transfer details by ID")
    public ResponseEntity<ApiResponse<TransferDTO>> getTransferById(
            @PathVariable Long id,
            Authentication authentication) {
        TransferDTO transfer = transferService.getTransferById(id, authentication);
        return ResponseEntity.ok(ApiResponse.of(transfer));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'BASE_COMMANDER', 'LOGISTICS_OFFICER')")
    @Operation(summary = "Update transfer status (e.g., in_transit, completed, cancelled)")
    public ResponseEntity<ApiResponse<TransferDTO>> updateTransferStatus(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateTransferStatusRequest body,
            @RequestParam(value = "status", required = false) TransferStatus statusParam,
            Authentication authentication) {

        TransferStatus newStatus = (body != null && body.getStatus() != null)
                ? body.getStatus()
                : statusParam;

        TransferDTO updated = transferService.updateTransferStatus(id, newStatus, authentication);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }
}
