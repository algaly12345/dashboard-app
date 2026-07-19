package com.realestate.admin.dto.api;

import com.realestate.admin.entity.Estate;

import java.time.LocalDateTime;

/** Lightweight projection used by list endpoints - keeps API payloads small. */
public record EstateSummaryDto(
        Long id,
        String title,
        String shortDescription,
        String city,
        String districts,
        String advertisementType,
        String estateType,
        String price,
        String status,
        boolean hasVirtualTour,
        String firstImage,
        LocalDateTime createdAt
) {
    public static EstateSummaryDto from(Estate e) {
        return new EstateSummaryDto(
                e.getId(), e.getTitle(), e.getShortDescription(), e.getCity(), e.getDistricts(),
                e.getAdvertisementType(), e.getEstateType(), e.getPrice(),
                e.getStatus() != null ? e.getStatus().name() : null,
                e.isHasVirtualTour(), e.getFirstImage(), e.getCreatedAt());
    }
}
