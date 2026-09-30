package com.mams.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @JsonProperty("new_password")
    private String newPassword;

    @JsonProperty("newPassword")
    public String getNewPasswordCamel() {
        return newPassword;
    }

    @JsonProperty("newPassword")
    public void setNewPasswordCamel(String newPassword) {
        this.newPassword = newPassword;
    }
}
