package com.realestate.admin.dto.api;

import com.realestate.admin.entity.Estate;

import java.time.LocalDateTime;

/** Full projection used by the single-listing endpoint. */
public record EstateDetailDto(
        Long id,
        String title,
        String shortDescription,
        String longDescription,
        String ownershipType,
        String categoryName,
        String advertisementType,
        String estateType,
        String propertyType,
        String status,
        String price,
        String totalPrice,
        String priceNegotiation,
        String space,
        Integer floors,
        String ageEstate,
        String city,
        String districts,
        String address,
        String latitude,
        String longitude,
        String videoUrl,
        String arPath,
        boolean hasVirtualTour,
        String images,
        String advertiserName,
        String phoneNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EstateDetailDto from(Estate e) {
        return new EstateDetailDto(
                e.getId(), e.getTitle(), e.getShortDescription(), e.getLongDescription(),
                e.getOwnershipType(), e.getCategoryName(), e.getAdvertisementType(), e.getEstateType(),
                e.getPropertyType(), e.getStatus() != null ? e.getStatus().name() : null,
                e.getPrice(), e.getTotalPrice(), e.getPriceNegotiation(), e.getSpace(), e.getFloors(),
                e.getAgeEstate(), e.getCity(), e.getDistricts(), e.getAddress(), e.getLatitude(), e.getLongitude(),
                e.getVideoUrl(), e.getArPath(), e.isHasVirtualTour(), e.getImages(),
                e.getAdvertiserName(), e.getPhoneNumber(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
