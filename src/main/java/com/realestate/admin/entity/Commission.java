package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commissions")
@Getter
@Setter
@NoArgsConstructor
public class Commission {

    @Id
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "referral_id")
    private Long referralId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "available_at")
    private LocalDateTime availableAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status { PENDING, APPROVED, AVAILABLE, WITHDRAWN, CANCELLED }
}
