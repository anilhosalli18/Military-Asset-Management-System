package com.mams.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetMovementDetailDTO {

    private List<PurchaseLineItemDTO> purchases;

    @JsonProperty("transfers_in")
    private List<TransferLineItemDTO> transfersIn;

    @JsonProperty("transfers_out")
    private List<TransferLineItemDTO> transfersOut;

    @JsonProperty("transfersIn")
    public List<TransferLineItemDTO> getTransfersInCamel() {
        return transfersIn;
    }

    @JsonProperty("transfersOut")
    public List<TransferLineItemDTO> getTransfersOutCamel() {
        return transfersOut;
    }
}
