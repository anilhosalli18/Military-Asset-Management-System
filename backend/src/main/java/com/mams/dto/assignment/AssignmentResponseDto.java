package com.mams.dto.assignment;

import com.mams.model.enums.AssignmentStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResponseDto {
    private Long id;
    private Long baseId;
    private String baseName;
    private Long equipmentTypeId;
    private String equipmentTypeName;
    private String personnelName;
    private String personnelIdNo;
    private Integer quantity;
    private AssignmentStatus status;
    private LocalDate assignedDate;
    private LocalDate expendedDate;
    private LocalDate returnedDate;
    private String notes;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
