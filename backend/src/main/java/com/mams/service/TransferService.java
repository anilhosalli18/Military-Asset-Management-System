package com.mams.service;

import com.mams.dto.transfer.CreateTransferRequest;
import com.mams.dto.transfer.TransferDTO;
import com.mams.model.enums.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;

public interface TransferService {
    TransferDTO createTransfer(CreateTransferRequest request, Authentication auth);
    Page<TransferDTO> getTransfers(LocalDate startDate, LocalDate endDate, Long baseId, Long equipmentTypeId, String direction, Pageable pageable, Authentication auth);
    TransferDTO getTransferById(Long id, Authentication auth);
    TransferDTO updateTransferStatus(Long id, TransferStatus newStatus, Authentication auth);
}
