package com.mams.dto.purchase;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseDTO {

    private Long id;

    @JsonProperty("base_id")
    private Long baseId;

    @JsonProperty("base_name")
    private String baseName;

    @JsonProperty("equipment_type_id")
    private Long equipmentTypeId;

    @JsonProperty("equipment_name")
    private String equipmentName;

    private Integer quantity;

    @JsonProperty("unit_cost")
    private BigDecimal unitCost;

    @JsonProperty("total_cost")
    private BigDecimal totalCost;

    private String vendor;

    @JsonProperty("purchase_date")
    private LocalDate purchaseDate;

    private String notes;

    @JsonProperty("created_by")
    private Long createdBy;

    @JsonProperty("created_by_username")
    private String createdByUsername;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    // camelCase getters for frontend compatibility
    @JsonProperty("baseId")
    public Long getBaseIdCamel() {
        return baseId;
    }

    @JsonProperty("baseName")
    public String getBaseNameCamel() {
        return baseName;
    }

    @JsonProperty("equipmentTypeId")
    public Long getEquipmentTypeIdCamel() {
        return equipmentTypeId;
    }

    @JsonProperty("equipmentName")
    public String getEquipmentNameCamel() {
        return equipmentName;
    }

    @JsonProperty("unitCost")
    public BigDecimal getUnitCostCamel() {
        return unitCost;
    }

    @JsonProperty("totalCost")
    public BigDecimal getTotalCostCamel() {
        return totalCost;
    }

    @JsonProperty("purchaseDate")
    public LocalDate getPurchaseDateCamel() {
        return purchaseDate;
    }

    @JsonProperty("createdBy")
    public Long getCreatedByCamel() {
        return createdBy;
    }

    @JsonProperty("createdByUsername")
    public String getCreatedByUsernameCamel() {
        return createdByUsername;
    }

    @JsonProperty("createdAt")
    public LocalDateTime getCreatedAtCamel() {
        return createdAt;
    }
}
