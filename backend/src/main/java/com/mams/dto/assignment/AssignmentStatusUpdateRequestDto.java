package com.mams.dto.assignment;

import com.mams.model.enums.AssignmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentStatusUpdateRequestDto {

    @NotNull(message = "Assignment status is required")
    private AssignmentStatus status;

    private LocalDate actionDate;

    private String notes;
}
