package com.mams.service;

import com.mams.dto.dashboard.DashboardMetricsDTO;
import com.mams.dto.dashboard.NetMovementDetailDTO;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;

public interface DashboardService {

    DashboardMetricsDTO getMetrics(LocalDate startDate,
                                   LocalDate endDate,
                                   Long baseId,
                                   Long equipmentTypeId,
                                   Authentication auth);

    NetMovementDetailDTO getNetMovementDetail(LocalDate startDate,
                                             LocalDate endDate,
                                             Long baseId,
                                             Long equipmentTypeId,
                                             Authentication auth);
}
