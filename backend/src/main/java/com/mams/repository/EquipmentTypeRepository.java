package com.mams.repository;

import com.mams.model.EquipmentType;
import com.mams.model.enums.EquipmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, Long> {
    List<EquipmentType> findByCategory(EquipmentCategory category);
}
