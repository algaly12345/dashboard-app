package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_types")
@Getter
@Setter
@NoArgsConstructor
public class ServiceType {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;
}
