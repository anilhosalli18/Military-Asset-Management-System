package com.mams.dto.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mams.model.enums.TransferStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTransferRequest {

    @NotNull(message = "Equipment type ID is required")
    @JsonProperty("equipment_type_id")
    private Long equipmentTypeId;

    @NotNull(message = "From base ID is required")
    @JsonProperty("from_base_id")
    private Long fromBaseId;

    @NotNull(message = "To base ID is required")
    @JsonProperty("to_base_id")
    private Long toBaseId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    @NotNull(message = "Transfer date is required")
    @PastOrPresent(message = "Transfer date cannot be in the future")
    @JsonProperty("transfer_date")
    private LocalDate transferDate;

    @Builder.Default
    private TransferStatus status = TransferStatus.completed;

    private String notes;

    // Dual camelCase property support for Jackson deserialization
    @JsonProperty("equipmentTypeId")
    public Long getEquipmentTypeIdCamel() {
        return equipmentTypeId;
    }

    @JsonProperty("equipmentTypeId")
    public void setEquipmentTypeIdCamel(Long equipmentTypeId) {
        this.equipmentTypeId = equipmentTypeId;
    }

    @JsonProperty("fromBaseId")
    public Long getFromBaseIdCamel() {
        return fromBaseId;
    }

    @JsonProperty("fromBaseId")
    public void setFromBaseIdCamel(Long fromBaseId) {
        this.fromBaseId = fromBaseId;
    }

    @JsonProperty("toBaseId")
    public Long getToBaseIdCamel() {
        return toBaseId;
    }

    @JsonProperty("toBaseId")
    public void setToBaseIdCamel(Long toBaseId) {
        this.toBaseId = toBaseId;
    }

    @JsonProperty("transferDate")
    public LocalDate getTransferDateCamel() {
        return transferDate;
    }

    @JsonProperty("transferDate")
    public void setTransferDateCamel(LocalDate transferDate) {
        this.transferDate = transferDate;
    }
}
