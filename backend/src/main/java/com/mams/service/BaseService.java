package com.mams.service;

import com.mams.dto.base.BaseDto;
import com.mams.model.Base;
import com.mams.repository.BaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BaseService {

    private final BaseRepository baseRepository;

    public BaseService(BaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Transactional(readOnly = true)
    public List<BaseDto> getActiveBases() {
        return baseRepository.findByIsActiveTrue().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BaseDto> getAllBases() {
        return baseRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    private BaseDto mapToDto(Base base) {
        return BaseDto.builder()
                .id(base.getId())
                .name(base.getName())
                .location(base.getLocation())
                .isActive(base.getIsActive())
                .build();
    }
}
