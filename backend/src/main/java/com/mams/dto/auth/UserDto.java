package com.mams.dto.auth;

import com.mams.model.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private Long baseId;
    private String baseName;
    private Boolean isActive;
}
