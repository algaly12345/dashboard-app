package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
public class Agent {

    @Id
    private Integer id;

    private String name;
    private String phone;
    private String identity;

    @Column(name = "unified_number")
    private String unifiedNumber;

    @Column(name = "advertiser_no")
    private String advertiserNo;

    @Column(name = "agent_type", nullable = false)
    private String agentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false)
    private MembershipType membershipType;

    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "fal_license_number")
    private String falLicenseNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum MembershipType { agent, provider, customer }
}
