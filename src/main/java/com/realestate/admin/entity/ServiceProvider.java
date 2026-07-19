package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_providers")
@Getter
@Setter
@NoArgsConstructor
public class ServiceProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identity_number")
    private String identityNumber;

    @Column(name = "identity_type")
    private String identityType;

    @Column(name = "service_type_id", nullable = false)
    private Long serviceTypeId;

    private String image;

    @Column(nullable = false)
    private String address;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String job;

    @Column(name = "zone_id")
    private Integer zoneId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
