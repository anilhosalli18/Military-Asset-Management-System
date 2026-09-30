package com.mams.controller;

import com.mams.dto.common.ApiResponse;
import com.mams.dto.dashboard.DashboardMetricsDTO;
import com.mams.dto.dashboard.NetMovementDetailDTO;
import com.mams.exception.BadRequestException;
import com.mams.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Endpoints for asset metrics and movements")
@SecurityRequirement(name = "BearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get derived metrics (opening/closing balances, net movement, purchases, transfers, expenditures)")
    public ResponseEntity<ApiResponse<DashboardMetricsDTO>> getMetrics(
            @RequestParam(value = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateParam,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateCamel,
            @RequestParam(value = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateParam,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateCamel,
            @RequestParam(value = "base_id", required = false) Long baseIdParam,
            @RequestParam(value = "baseId", required = false) Long baseIdCamel,
            @RequestParam(value = "equipment_type_id", required = false) Long equipmentTypeIdParam,
            @RequestParam(value = "equipmentTypeId", required = false) Long equipmentTypeIdCamel,
            Authentication authentication) {

        LocalDate start = startDateParam != null ? startDateParam : startDateCamel;
        LocalDate end = endDateParam != null ? endDateParam : endDateCamel;
        Long baseId = baseIdParam != null ? baseIdParam : baseIdCamel;
        Long equipmentTypeId = equipmentTypeIdParam != null ? equipmentTypeIdParam : equipmentTypeIdCamel;

        validateDateRange(start, end);

        DashboardMetricsDTO metrics = dashboardService.getMetrics(start, end, baseId, equipmentTypeId, authentication);
        return ResponseEntity.ok(ApiResponse.of(metrics));
    }

    @GetMapping("/net-movement-detail")
    @Operation(summary = "Get line items for purchases, transfers in, and transfers out for drill-down modal")
    public ResponseEntity<ApiResponse<NetMovementDetailDTO>> getNetMovementDetail(
            @RequestParam(value = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateParam,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateCamel,
            @RequestParam(value = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateParam,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateCamel,
            @RequestParam(value = "base_id", required = false) Long baseIdParam,
            @RequestParam(value = "baseId", required = false) Long baseIdCamel,
            @RequestParam(value = "equipment_type_id", required = false) Long equipmentTypeIdParam,
            @RequestParam(value = "equipmentTypeId", required = false) Long equipmentTypeIdCamel,
            Authentication authentication) {

        LocalDate start = startDateParam != null ? startDateParam : startDateCamel;
        LocalDate end = endDateParam != null ? endDateParam : endDateCamel;
        Long baseId = baseIdParam != null ? baseIdParam : baseIdCamel;
        Long equipmentTypeId = equipmentTypeIdParam != null ? equipmentTypeIdParam : equipmentTypeIdCamel;

        validateDateRange(start, end);

        NetMovementDetailDTO detail = dashboardService.getNetMovementDetail(start, end, baseId, equipmentTypeId, authentication);
        return ResponseEntity.ok(ApiResponse.of(detail));
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Both start_date and end_date query parameters are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("start_date cannot be after end_date");
        }
    }
}
