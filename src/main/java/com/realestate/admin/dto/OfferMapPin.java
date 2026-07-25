package com.realestate.admin.dto;

import com.realestate.admin.entity.Offer;

/** Lightweight projection for the dashboard services map. */
public record OfferMapPin(
        Long id,
        String title,
        String latitude,
        String longitude,
        String image,
        String offerType,
        Integer servicePrice,
        Integer discount,
        String status
) {
    public static OfferMapPin from(Offer o) {
        return new OfferMapPin(
                o.getId(), o.getTitle(),
                o.getLatitude() != null ? o.getLatitude().toString() : null,
                o.getLongitude() != null ? o.getLongitude().toString() : null,
                o.getImage(),
                o.getOfferType() != null ? o.getOfferType().name() : null,
                o.getServicePrice(), o.getDiscount(), o.getStatus());
    }
}
