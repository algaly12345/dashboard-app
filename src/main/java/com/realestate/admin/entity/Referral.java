package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "referrals")
@Getter
@Setter
@NoArgsConstructor
public class Referral {

    @Id
    private Long id;

    @Column(name = "referrer_id")
    private Long referrerId;

    @Column(name = "referred_id")
    private Long referredId;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status { EXPIRED, REJECTED, COMPLETED, PENDING_PAYMENT }
}
