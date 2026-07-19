package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Maps to `category_offer` — links a service offer to a real-estate category. */
@Entity
@Table(name = "category_offer")
@Getter
@Setter
@NoArgsConstructor
public class CategoryOffer {

    @Id
    private Integer id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;
}
