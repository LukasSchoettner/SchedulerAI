package com.scheduler.customermanagement.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "minimum_requirements")
@Getter
@Setter
@NoArgsConstructor
public class MinimumRequirement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;
    private String type;
    private Integer amount;
    private String period;
}
