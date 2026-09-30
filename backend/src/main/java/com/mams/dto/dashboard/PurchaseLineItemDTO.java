package com.mams.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseLineItemDTO {
    private Long id;

    @JsonProperty("purchaseDate")
    private LocalDate purchaseDate;

    private Integer quantity;
    private String vendor;

    @JsonProperty("baseName")
    private String baseName;

    @JsonProperty("equipmentName")
    private String equipmentName;

    @JsonProperty("purchase_date")
    public LocalDate getPurchaseDateSnake() {
        return purchaseDate;
    }

    @JsonProperty("base_name")
    public String getBaseNameSnake() {
        return baseName;
    }

    @JsonProperty("equipment_name")
    public String getEquipmentNameSnake() {
        return equipmentName;
    }
}
