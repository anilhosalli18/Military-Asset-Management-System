package com.mams.controller;

import com.mams.dto.assignment.AssignmentDTO;
import com.mams.dto.assignment.CreateAssignmentRequest;
import com.mams.dto.assignment.ExpendRequest;
import com.mams.dto.assignment.ReturnRequest;
import com.mams.dto.common.ApiResponse;
import com.mams.service.AssignmentExpenditureService;
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
@RequestMapping("/api/assignments")
@Tag(name = "Assignments & Expenditures", description = "Endpoints for asset assignment to personnel and expenditure tracking. (LOGISTICS_OFFICER denied)")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN') or hasRole('BASE_COMMANDER')")
public class AssignmentExpenditureController {

    private final AssignmentExpenditureService assignmentService;

    public AssignmentExpenditureController(AssignmentExpenditureService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    @Operation(summary = "Assign asset to personnel (ADMIN & BASE_COMMANDER only)")
    public ResponseEntity<ApiResponse<AssignmentDTO>> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request,
            Authentication authentication) {
        AssignmentDTO created = assignmentService.createAssignment(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @GetMapping
    @Operation(summary = "List assignments and expenditures with filters (ADMIN & BASE_COMMANDER only)")
    public ResponseEntity<ApiResponse<Page<AssignmentDTO>>> getAssignments(
            @RequestParam(required = false) String status,
            @RequestParam(value = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateParam,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateCamel,
            @RequestParam(value = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateParam,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateCamel,
            @RequestParam(value = "base_id", required = false) Long baseIdParam,
            @RequestParam(value = "baseId", required = false) Long baseIdCamel,
            @RequestParam(value = "equipment_type_id", required = false) Long equipmentTypeIdParam,
            @RequestParam(value = "equipmentTypeId", required = false) Long equipmentTypeIdCamel,
            @PageableDefault(size = 10, sort = "assignedDate") Pageable pageable,
            Authentication authentication) {

        LocalDate startDate = startDateParam != null ? startDateParam : startDateCamel;
        LocalDate endDate = endDateParam != null ? endDateParam : endDateCamel;
        Long baseId = baseIdParam != null ? baseIdParam : baseIdCamel;
        Long equipmentTypeId = equipmentTypeIdParam != null ? equipmentTypeIdParam : equipmentTypeIdCamel;

        Page<AssignmentDTO> assignments = assignmentService.getAssignments(
                status, startDate, endDate, baseId, equipmentTypeId, pageable, authentication);
        return ResponseEntity.ok(ApiResponse.of(assignments));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get assignment detail by ID (ADMIN & BASE_COMMANDER only)")
    public ResponseEntity<ApiResponse<AssignmentDTO>> getAssignmentById(
            @PathVariable Long id,
            Authentication authentication) {
        AssignmentDTO assignment = assignmentService.getAssignmentById(id, authentication);
        return ResponseEntity.ok(ApiResponse.of(assignment));
    }

    @PatchMapping("/{id}/expend")
    @Operation(summary = "Mark an assignment as expended (ADMIN & BASE_COMMANDER only)")
    public ResponseEntity<ApiResponse<AssignmentDTO>> markExpended(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ExpendRequest request,
            Authentication authentication) {

        LocalDate expendedDate = request != null ? request.getExpendedDate() : null;
        String notes = request != null ? request.getNotes() : null;

        AssignmentDTO updated = assignmentService.markExpended(id, expendedDate, notes, authentication);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }

    @PatchMapping("/{id}/return")
    @Operation(summary = "Mark an assigned asset as returned (ADMIN & BASE_COMMANDER only)")
    public ResponseEntity<ApiResponse<AssignmentDTO>> markReturned(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReturnRequest request,
            Authentication authentication) {

        LocalDate returnedDate = request != null ? request.getReturnedDate() : null;
        String notes = request != null ? request.getNotes() : null;

        AssignmentDTO updated = assignmentService.markReturned(id, returnedDate, notes, authentication);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }
}
