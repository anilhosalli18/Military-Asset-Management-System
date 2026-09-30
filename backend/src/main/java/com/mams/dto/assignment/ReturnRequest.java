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
public class ReturnRequest {

    @PastOrPresent(message = "Returned date cannot be in the future")
    @JsonProperty("returned_date")
    private LocalDate returnedDate;

    private String notes;

    @JsonProperty("returnedDate")
    public LocalDate getReturnedDateCamel() {
        return returnedDate;
    }

    @JsonProperty("returnedDate")
    public void setReturnedDateCamel(LocalDate returnedDate) {
        this.returnedDate = returnedDate;
    }
}
