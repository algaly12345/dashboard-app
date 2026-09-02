package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_withdrawal_requests")
@Getter
@Setter
@NoArgsConstructor
public class CommissionWithdrawalRequest {

    @Id
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private BigDecimal amount;
    @Column(name = "account_holder_name")
    private String accountHolderName;
    private String iban;
    @Column(name = "bank_name")
    private String bankName;
    @Column(name = "national_id")
    private String nationalId;





    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status { pending, approved, rejected, paid }
}
