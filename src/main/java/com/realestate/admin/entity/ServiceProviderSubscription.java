package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_provider_subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class ServiceProviderSubscription {

    @Id
    private Integer id;

    @Column(name = "subscription_number")
    private String subscriptionNumber;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "service_plan_id")
    private Integer servicePlanId;

    private Integer duration;

    @Column(name = "expiry_date")
    private String expiryDate;

    @Column(name = "subscription_status")
    private String subscriptionStatus;

    @Column(name = "payment_status")
    private String paymentStatus;

    private String price;

    @Column(name = "base_price")
    private BigDecimal basePrice;

    @Column(name = "extra_zones_cost")
    private BigDecimal extraZonesCost;

    @Column(name = "extra_categories_count")
    private Integer extraCategoriesCount;

    @Column(name = "extra_categories_cost")
    private BigDecimal extraCategoriesCost;

    @Column(name = "monthly_total")
    private BigDecimal monthlyTotal;

    @Column(name = "discount_percent")
    private Integer discountPercent;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "offer_id")
    private Integer offerId;

    @Column(name = "number_of_ads")
    private Integer numberOfAds;

    @Column(name = "number_of_zone")
    private Integer numberOfZone;

    @Column(name = "number_of_categories")
    private Integer numberOfCategories;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
