package com.mams.dto.equipment;

import com.mams.model.enums.EquipmentCategory;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentTypeDto {
    private Long id;
    private String name;
    private EquipmentCategory category;
    private String unit;
}
