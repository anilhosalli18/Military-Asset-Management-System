package com.mams.service;

import com.mams.dto.assignment.AssignmentDTO;
import com.mams.dto.assignment.CreateAssignmentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;

public interface AssignmentExpenditureService {
    AssignmentDTO createAssignment(CreateAssignmentRequest request, Authentication auth);
    Page<AssignmentDTO> getAssignments(String status, LocalDate startDate, LocalDate endDate, Long baseId, Long equipmentTypeId, Pageable pageable, Authentication auth);
    AssignmentDTO getAssignmentById(Long id, Authentication auth);
    AssignmentDTO markExpended(Long id, LocalDate expendedDate, String notes, Authentication auth);
    AssignmentDTO markReturned(Long id, LocalDate returnedDate, String notes, Authentication auth);
}
