package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Single-row settings table for the referral program - never insert a
 *  second row, only ever update the existing one (or create it once if
 *  the table is empty). */
@Entity
@Table(name = "referral_settings")
@Getter
@Setter
@NoArgsConstructor
public class ReferralSettings {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type")
    private RewardType rewardType;

    @Column(name = "reward_value")
    private BigDecimal rewardValue;

    @Column(name = "attribution_window_days")
    private Integer attributionWindowDays;

    @Column(name = "min_payout_limit")
    private BigDecimal minPayoutLimit;

    @Column(name = "commission_hold_days")
    private Integer commissionHoldDays;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum RewardType { PERCENTAGE, FIXED }
}
