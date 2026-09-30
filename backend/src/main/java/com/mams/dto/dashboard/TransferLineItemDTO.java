package com.mams.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferLineItemDTO {
    private Long id;

    @JsonProperty("transferDate")
    private LocalDate transferDate;

    private Integer quantity;

    @JsonProperty("fromBaseName")
    private String fromBaseName;

    @JsonProperty("toBaseName")
    private String toBaseName;

    @JsonProperty("equipmentName")
    private String equipmentName;

    @JsonProperty("transfer_date")
    public LocalDate getTransferDateSnake() {
        return transferDate;
    }

    @JsonProperty("from_base_name")
    public String getFromBaseNameSnake() {
        return fromBaseName;
    }

    @JsonProperty("to_base_name")
    public String getToBaseNameSnake() {
        return toBaseName;
    }

    @JsonProperty("equipment_name")
    public String getEquipmentNameSnake() {
        return equipmentName;
    }
}
