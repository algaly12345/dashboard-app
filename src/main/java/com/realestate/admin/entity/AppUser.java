package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps to the `users` table. This is the single account table for every
 * person using the mobile app: a property seeker, a service provider or
 * an advertiser/agent. `userType` tells them apart and `agents` /
 * `service_providers` hold the role-specific profile data (1-1 on user_id).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    private String email;

    private String image;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_active", nullable = false)
    private Status isActive;

    /** e.g. customer / provider — role granularity mostly lives in `agents.membership_type` */
    @Column(name = "user_type", nullable = false)
    private String userType;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "wallet_balance")
    private BigDecimal walletBalance;

    @Column(name = "membership_type")
    private String membershipType;

    @Column(name = "fal_license_number")
    private String falLicenseNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status { active, disactive }
}
