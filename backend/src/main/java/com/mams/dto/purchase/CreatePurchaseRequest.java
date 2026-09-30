package com.mams.dto.purchase;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePurchaseRequest {

    @NotNull(message = "Base ID is required")
    @JsonProperty("base_id")
    private Long baseId;

    @NotNull(message = "Equipment type ID is required")
    @JsonProperty("equipment_type_id")
    private Long equipmentTypeId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @JsonProperty("unit_cost")
    private BigDecimal unitCost;

    private String vendor;

    @NotNull(message = "Purchase date is required")
    @PastOrPresent(message = "Purchase date cannot be in the future")
    @JsonProperty("purchase_date")
    private LocalDate purchaseDate;

    private String notes;

    @JsonProperty("baseId")
    public Long getBaseIdCamel() {
        return baseId;
    }

    @JsonProperty("baseId")
    public void setBaseIdCamel(Long baseId) {
        this.baseId = baseId;
    }

    @JsonProperty("equipmentTypeId")
    public Long getEquipmentTypeIdCamel() {
        return equipmentTypeId;
    }

    @JsonProperty("equipmentTypeId")
    public void setEquipmentTypeIdCamel(Long equipmentTypeId) {
        this.equipmentTypeId = equipmentTypeId;
    }

    @JsonProperty("unitCost")
    public BigDecimal getUnitCostCamel() {
        return unitCost;
    }

    @JsonProperty("unitCost")
    public void setUnitCostCamel(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    @JsonProperty("purchaseDate")
    public LocalDate getPurchaseDateCamel() {
        return purchaseDate;
    }

    @JsonProperty("purchaseDate")
    public void setPurchaseDateCamel(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
}
