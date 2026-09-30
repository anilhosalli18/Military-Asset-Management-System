package com.mams.dto.transfer;

import com.mams.model.enums.TransferStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTransferStatusRequest {

    @NotNull(message = "Status is required")
    private TransferStatus status;
}
