package com.mams.dto.assignment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
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
public class CreateAssignmentRequest {

    @JsonProperty("base_id")
    private Long baseId;

    @NotNull(message = "Equipment type ID is required")
    @JsonProperty("equipment_type_id")
    private Long equipmentTypeId;

    @NotBlank(message = "Personnel name is required")
    @JsonProperty("personnel_name")
    private String personnelName;

    @JsonProperty("personnel_id_no")
    private String personnelIdNo;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    @NotNull(message = "Assigned date is required")
    @PastOrPresent(message = "Assigned date cannot be in the future")
    @JsonProperty("assigned_date")
    private LocalDate assignedDate;

    private String notes;

    // Dual camelCase property getters & setters for Jackson compatibility
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

    @JsonProperty("personnelName")
    public String getPersonnelNameCamel() {
        return personnelName;
    }

    @JsonProperty("personnelName")
    public void setPersonnelNameCamel(String personnelName) {
        this.personnelName = personnelName;
    }

    @JsonProperty("personnelIdNo")
    public String getPersonnelIdNoCamel() {
        return personnelIdNo;
    }

    @JsonProperty("personnelIdNo")
    public void setPersonnelIdNoCamel(String personnelIdNo) {
        this.personnelIdNo = personnelIdNo;
    }

    @JsonProperty("assignedDate")
    public LocalDate getAssignedDateCamel() {
        return assignedDate;
    }

    @JsonProperty("assignedDate")
    public void setAssignedDateCamel(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }
}
