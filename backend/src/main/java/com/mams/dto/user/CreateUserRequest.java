package com.mams.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mams.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @JsonProperty("full_name")
    private String fullName;

    @NotNull(message = "Role is required")
    private Role role;

    @JsonProperty("base_id")
    private Long baseId;

    @JsonProperty("fullName")
    public String getFullNameCamel() {
        return fullName;
    }

    @JsonProperty("fullName")
    public void setFullNameCamel(String fullName) {
        this.fullName = fullName;
    }

    @JsonProperty("baseId")
    public Long getBaseIdCamel() {
        return baseId;
    }

    @JsonProperty("baseId")
    public void setBaseIdCamel(Long baseId) {
        this.baseId = baseId;
    }
}
