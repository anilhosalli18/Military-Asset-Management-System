package com.mams.service;

import com.mams.dto.equipment.EquipmentTypeDto;
import com.mams.model.EquipmentType;
import com.mams.repository.EquipmentTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EquipmentTypeService {

    private final EquipmentTypeRepository equipmentTypeRepository;

    public EquipmentTypeService(EquipmentTypeRepository equipmentTypeRepository) {
        this.equipmentTypeRepository = equipmentTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<EquipmentTypeDto> getAllEquipmentTypes() {
        return equipmentTypeRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    private EquipmentTypeDto mapToDto(EquipmentType equipment) {
        return EquipmentTypeDto.builder()
                .id(equipment.getId())
                .name(equipment.getName())
                .category(equipment.getCategory())
                .unit(equipment.getUnit())
                .build();
    }
}
