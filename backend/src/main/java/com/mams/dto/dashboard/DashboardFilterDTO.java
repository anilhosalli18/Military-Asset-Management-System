package com.mams.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardFilterDTO {
    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    @JsonProperty("base_id")
    private Long baseId;

    @JsonProperty("equipment_type_id")
    private Long equipmentTypeId;

    @JsonProperty("startDate")
    public LocalDate getStartDateCamel() {
        return startDate;
    }

    @JsonProperty("endDate")
    public LocalDate getEndDateCamel() {
        return endDate;
    }

    @JsonProperty("baseId")
    public Long getBaseIdCamel() {
        return baseId;
    }

    @JsonProperty("equipmentTypeId")
    public Long getEquipmentTypeIdCamel() {
        return equipmentTypeId;
    }
}
