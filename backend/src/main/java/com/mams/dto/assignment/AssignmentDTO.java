package com.mams.dto.assignment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mams.model.enums.AssignmentStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentDTO {

    private Long id;

    @JsonProperty("base_id")
    private Long baseId;

    @JsonProperty("base_name")
    private String baseName;

    @JsonProperty("equipment_type_id")
    private Long equipmentTypeId;

    @JsonProperty("equipment_name")
    private String equipmentName;

    @JsonProperty("personnel_name")
    private String personnelName;

    @JsonProperty("personnel_id_no")
    private String personnelIdNo;

    private Integer quantity;

    private AssignmentStatus status;

    @JsonProperty("assigned_date")
    private LocalDate assignedDate;

    @JsonProperty("expended_date")
    private LocalDate expendedDate;

    @JsonProperty("returned_date")
    private LocalDate returnedDate;

    private String notes;

    @JsonProperty("created_by")
    private Long createdBy;

    @JsonProperty("created_by_username")
    private String createdByUsername;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    // Dual camelCase property getters for frontend compatibility
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

    @JsonProperty("personnelName")
    public String getPersonnelNameCamel() {
        return personnelName;
    }

    @JsonProperty("personnelIdNo")
    public String getPersonnelIdNoCamel() {
        return personnelIdNo;
    }

    @JsonProperty("assignedDate")
    public LocalDate getAssignedDateCamel() {
        return assignedDate;
    }

    @JsonProperty("expendedDate")
    public LocalDate getExpendedDateCamel() {
        return expendedDate;
    }

    @JsonProperty("returnedDate")
    public LocalDate getReturnedDateCamel() {
        return returnedDate;
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

    @JsonProperty("updatedAt")
    public LocalDateTime getUpdatedAtCamel() {
        return updatedAt;
    }
}
