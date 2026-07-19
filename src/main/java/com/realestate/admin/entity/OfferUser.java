package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps to `offer_user`. Each offer appears with exactly one user in the
 * sample data, so this is treated as "who submitted this offer" with
 * `status` as its admin moderation state (pending/accept/reject) - used
 * here to count how many service offers a given provider has added.
 */
@Entity
@Table(name = "offer_user")
@Getter
@Setter
@NoArgsConstructor
public class OfferUser {

    @Id
    private Integer id;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public enum Status { pending, accept, reject }
}
