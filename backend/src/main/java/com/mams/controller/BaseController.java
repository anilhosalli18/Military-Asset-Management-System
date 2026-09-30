package com.mams.controller;

import com.mams.dto.base.BaseDto;
import com.mams.service.BaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bases")
@Tag(name = "Bases", description = "Base management and lookup endpoints")
@SecurityRequirement(name = "BearerAuth")
public class BaseController {

    private final BaseService baseService;

    public BaseController(BaseService baseService) {
        this.baseService = baseService;
    }

    @GetMapping
    @Operation(summary = "Get list of active military bases")
    public ResponseEntity<List<BaseDto>> getBases() {
        return ResponseEntity.ok(baseService.getActiveBases());
    }
}
