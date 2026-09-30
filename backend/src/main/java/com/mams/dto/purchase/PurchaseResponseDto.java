package com.mams.dto.purchase;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseResponseDto {
    private Long id;
    private Long baseId;
    private String baseName;
    private Long equipmentTypeId;
    private String equipmentTypeName;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String vendor;
    private LocalDate purchaseDate;
    private String notes;
    private String createdByUsername;
    private LocalDateTime createdAt;
}
