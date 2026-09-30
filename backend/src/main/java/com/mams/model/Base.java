package com.mams.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
