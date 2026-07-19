package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Maps to `offers` — a service offer published by a provider (price offer
 * or a discount offer). There is no direct user_id column; the creator is
 * identified by `phone_provider`, matched against `users.phone`.
 */
@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
public class Offer {

    @Id
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "expiry_date", nullable = false)
    private String expiryDate;

    @Column(name = "service_price")
    private Integer servicePrice;

    private String description;

    private String image;

    private Integer discount;

    @Column(name = "sended_at")
    private LocalDateTime sendedAt;

    @Column(name = "service_type_id", nullable = false)
    private Long serviceTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_type", nullable = false)
    private OfferType offerType;

    /** Plain varchar in the DB (not an enum) - seen values: pending, accept, reject. */
    @Column(nullable = false)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_owner", nullable = false)
    private OfferOwner offerOwner;

    @Column(name = "phone_provider")
    private String phoneProvider;

    public enum OfferType { price, discount }
    public enum OfferOwner { all, me }
}
