package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Maps to `offer_zone` — a service offer can cover several zones, and a
 * zone can host several offers. No `offers` table is mapped yet (its
 * schema wasn't provided), so this junction is used purely for counting:
 * "how many distinct offers serve this zone".
 */
@Entity
@Table(name = "offer_zone")
@Getter
@Setter
@NoArgsConstructor
@IdClass(OfferZone.OfferZoneId.class)
public class OfferZone {

    @Id
    @Column(name = "offer_id")
    private Long offerId;

    @Id
    @Column(name = "zone_id")
    private Long zoneId;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class OfferZoneId implements Serializable {
        private Long offerId;
        private Long zoneId;

        public OfferZoneId(Long offerId, Long zoneId) {
            this.offerId = offerId;
            this.zoneId = zoneId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OfferZoneId that)) return false;
            return Objects.equals(offerId, that.offerId) && Objects.equals(zoneId, that.zoneId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(offerId, zoneId);
        }
    }
}
