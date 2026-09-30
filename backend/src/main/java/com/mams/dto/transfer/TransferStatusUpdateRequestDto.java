package com.mams.dto.transfer;

import com.mams.model.enums.TransferStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferStatusUpdateRequestDto {

    @NotNull(message = "Transfer status is required")
    private TransferStatus status;

    private String notes;
}
