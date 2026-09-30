package com.mams.dto.base;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseDto {
    private Long id;
    private String name;
    private String location;
    private Boolean isActive;
}
