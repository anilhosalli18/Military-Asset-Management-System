package com.mams.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mams.model.enums.Role;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;
    private String username;
    private String email;

    @JsonProperty("full_name")
    private String fullName;

    private Role role;

    @JsonProperty("base_id")
    private Long baseId;

    @JsonProperty("base_name")
    private String baseName;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    // Dual camelCase property getters for frontend compatibility
    @JsonProperty("fullName")
    public String getFullNameCamel() {
        return fullName;
    }

    @JsonProperty("baseId")
    public Long getBaseIdCamel() {
        return baseId;
    }

    @JsonProperty("baseName")
    public String getBaseNameCamel() {
        return baseName;
    }

    @JsonProperty("isActive")
    public Boolean getIsActiveCamel() {
        return isActive;
    }

    @JsonProperty("createdAt")
    public LocalDateTime getCreatedAtCamel() {
        return createdAt;
    }
}
