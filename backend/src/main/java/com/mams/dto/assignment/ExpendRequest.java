package com.mams.dto.assignment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpendRequest {

    @PastOrPresent(message = "Expended date cannot be in the future")
    @JsonProperty("expended_date")
    private LocalDate expendedDate;

    private String notes;

    @JsonProperty("expendedDate")
    public LocalDate getExpendedDateCamel() {
        return expendedDate;
    }

    @JsonProperty("expendedDate")
    public void setExpendedDateCamel(LocalDate expendedDate) {
        this.expendedDate = expendedDate;
    }
}
