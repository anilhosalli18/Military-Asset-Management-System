package com.mams.model;

import com.mams.model.enums.EquipmentCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "equipment_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('vehicle','weapon','ammunition','other')")
    private EquipmentCategory category;

    @Column(nullable = false, length = 50)
    private String unit;
}
