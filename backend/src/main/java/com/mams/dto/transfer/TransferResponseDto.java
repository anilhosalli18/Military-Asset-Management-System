package com.mams.dto.transfer;

import com.mams.model.enums.TransferStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponseDto {
    private Long id;
    private Long equipmentTypeId;
    private String equipmentTypeName;
    private Long fromBaseId;
    private String fromBaseName;
    private Long toBaseId;
    private String toBaseName;
    private Integer quantity;
    private LocalDate transferDate;
    private TransferStatus status;
    private String notes;
    private String createdByUsername;
    private LocalDateTime createdAt;
}
