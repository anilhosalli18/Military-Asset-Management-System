package com.mams.dto.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mams.model.enums.TransferStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferDTO {

    private Long id;

    @JsonProperty("equipment_type_id")
    private Long equipmentTypeId;

    @JsonProperty("equipment_name")
    private String equipmentName;

    @JsonProperty("from_base_id")
    private Long fromBaseId;

    @JsonProperty("from_base_name")
    private String fromBaseName;

    @JsonProperty("to_base_id")
    private Long toBaseId;

    @JsonProperty("to_base_name")
    private String toBaseName;

    private Integer quantity;

    @JsonProperty("transfer_date")
    private LocalDate transferDate;

    private TransferStatus status;

    private String notes;

    @JsonProperty("created_by")
    private Long createdBy;

    @JsonProperty("created_by_username")
    private String createdByUsername;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    // Dual camelCase property getters for frontend compatibility
    @JsonProperty("equipmentTypeId")
    public Long getEquipmentTypeIdCamel() {
        return equipmentTypeId;
    }

    @JsonProperty("equipmentName")
    public String getEquipmentNameCamel() {
        return equipmentName;
    }

    @JsonProperty("fromBaseId")
    public Long getFromBaseIdCamel() {
        return fromBaseId;
    }

    @JsonProperty("fromBaseName")
    public String getFromBaseNameCamel() {
        return fromBaseName;
    }

    @JsonProperty("toBaseId")
    public Long getToBaseIdCamel() {
        return toBaseId;
    }

    @JsonProperty("toBaseName")
    public String getToBaseNameCamel() {
        return toBaseName;
    }

    @JsonProperty("transferDate")
    public LocalDate getTransferDateCamel() {
        return transferDate;
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
