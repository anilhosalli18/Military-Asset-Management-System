package com.mams.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mams.model.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @NotBlank(message = "Full name is required")
    @JsonProperty("full_name")
    private String fullName;

    @NotNull(message = "Role is required")
    private Role role;

    @JsonProperty("base_id")
    private Long baseId;

    @NotNull(message = "Active status is required")
    @JsonProperty("is_active")
    private Boolean isActive;

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

    @JsonProperty("isActive")
    public Boolean getIsActiveCamel() {
        return isActive;
    }

    @JsonProperty("isActive")
    public void setIsActiveCamel(Boolean isActive) {
        this.isActive = isActive;
    }
}
