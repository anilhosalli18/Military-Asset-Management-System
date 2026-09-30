package com.mams.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMetricsDTO {

    private DashboardFilterDTO filters;

    @JsonProperty("opening_balance")
    private Long openingBalance;

    @JsonProperty("closing_balance")
    private Long closingBalance;

    @JsonProperty("net_movement")
    private Long netMovement;

    private Long purchases;

    @JsonProperty("transfers_in")
    private Long transfersIn;

    @JsonProperty("transfers_out")
    private Long transfersOut;

    private Long assigned;
    private Long expended;

    @JsonProperty("openingBalance")
    public Long getOpeningBalanceCamel() {
        return openingBalance;
    }

    @JsonProperty("closingBalance")
    public Long getClosingBalanceCamel() {
        return closingBalance;
    }

    @JsonProperty("netMovement")
    public Long getNetMovementCamel() {
        return netMovement;
    }

    @JsonProperty("transfersIn")
    public Long getTransfersInCamel() {
        return transfersIn;
    }

    @JsonProperty("transfersOut")
    public Long getTransfersOutCamel() {
        return transfersOut;
    }
}
