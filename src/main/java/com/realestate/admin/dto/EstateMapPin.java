package com.realestate.admin.dto;

import com.realestate.admin.entity.Estate;

/** Lightweight projection for the map view - just what a marker/info-window/list card needs. */
public record EstateMapPin(
        Long id,
        String title,
        String price,
        String city,
        String districts,
        String latitude,
        String longitude,
        String firstImage,
        String advertiserName,
        String advertisementType
) {
    public static EstateMapPin from(Estate e) {
        return new EstateMapPin(
                e.getId(),
                e.getTitle() != null ? e.getTitle() : e.getShortDescription(),
                e.getPrice(), e.getCity(), e.getDistricts(),
                e.getLatitude(), e.getLongitude(), e.getFirstImage(),
                e.getAdvertiserName(), e.getAdvertisementType());
    }
}
