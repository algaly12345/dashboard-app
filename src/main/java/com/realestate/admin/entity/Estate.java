package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Maps to `estates`. Covers the columns used by the listing screen, the
 * filters, and the edit form. A handful of REGA/MOJ audit-only columns
 * (plan numbers duplicated under different keys, etc.) are still left out
 * on purpose - add them the same way if a future screen needs them.
 *
 * Naming note: PhysicalNamingStrategyStandardImpl is configured in
 * application.yml, so every @Column(name = "...") below is used exactly as
 * written - no case conversion, no quoting tricks needed even for the
 * genuinely-camelCase columns (advertiserName, titleDeedTypeName, etc).
 */
@Entity
@Table(name = "estates")
@Getter
@Setter
@NoArgsConstructor
public class Estate {

    @Id
    private Long id;

    @Column(name = "ownership_type")
    private String ownershipType;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "advertisement_type")
    private String advertisementType;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(name = "long_description")
    private String longDescription;

    private String address;

    /** Room-count JSON blob (bathrooms/bedrooms/lounges/kitchen counts). */
    private String property;

    private String space;

    @Column(nullable = false)
    private String price;

    private String planned;

    private Integer view;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String districts;

    private String qr;

    @Column(name = "images")
    private String images;

    /** Virtual-tour asset path/URL. Non-blank == listing has a 3D tour. */
    @Column(name = "ar_path")
    private String arPath;

    private String latitude;
    private String longitude;

    private String city;

    @Column(name = "other_advantages")
    private String otherAdvantages;

    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "territory_id")
    private Long territoryId;

    @Column(name = "age_estate")
    private String ageEstate;

    private String facilities;

    @Column(name = "price_negotiation")
    private String priceNegotiation;

    @Column(name = "national_address")
    private String nationalAddress;

    private Integer floors;

    @Column(name = "advertiser_no")
    private Integer advertiserNo;

    @Column(name = "ad_number")
    private String adNumber;

    private String title;

    @Column(name = "street_space")
    private String streetSpace;

    @Column(name = "build_space")
    private String buildSpace;

    @Column(name = "property_type")
    private String propertyType;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "video_url")
    private String videoUrl;

    private String feature;

    private String skyview;

    @Column(name = "estate_type")
    private String estateType;

    @Column(name = "authorization_number")
    private String authorizationNumber;

    @Column(name = "ad_license_number")
    private String adLicenseNumber;

    @Column(name = "creation_date")
    private String creationDate;

    @Column(name = "end_date")
    private String endDate;

    @Column(name = "deed_number")
    private String deedNumber;

    @Column(name = "adLicense_number")
    private String adLicenseNumberAlt;

    @Column(name = "brokerageAndMarketingLicenseNumber")
    private String brokerageAndMarketingLicenseNumber;

    @Column(name = "titleDeedTypeName")
    private String titleDeedTypeName;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "north_limit")
    private String northLimit;

    @Column(name = "east_limit")
    private String eastLimit;

    @Column(name = "west_limit")
    private String westLimit;

    @Column(name = "south_limit")
    private String southLimit;

    @Column(name = "street_width")
    private String streetWidth;

    @Column(name = "property_face")
    private String propertyFace;

    @Column(name = "total_price")
    private String totalPrice;

    @Column(name = "postal_code")
    private Integer postalCode;

    @Column(name = "plan_number")
    private String planNumber;

    @Column(name = "obligationsOnTheProperty")
    private String obligationsOnTheProperty;

    @Column(name = "guaranteesAndTheirDuration")
    private String guaranteesAndTheirDuration;

    @Column(name = "locationDescriptionOnMOJDeed")
    private String locationDescriptionOnMOJDeed;

    @Column(name = "numberOfRooms")
    private String numberOfRooms;

    @Column(name = "mainLandUseTypeName")
    private String mainLandUseTypeName;

    @Column(name = "landNumber")
    private String landNumber;

    @Column(name = "propertyUtilities")
    private String propertyUtilities;

    @Column(name = "propertyUsages")
    private String propertyUsages;

    @Column(name = "adLicenseUrl")
    private String adLicenseUrl;

    @Column(name = "advertiserName")
    private String advertiserName;

    @Column(name = "phoneNumber")
    private String phoneNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "isValid", nullable = false)
    private Integer isValid = 1;

    /** DB column is literally `identityـorـunified` (Arabic tatweel chars). */
    @Column(name = "identityـorـunified")
    private String identityOrUnified;

    public enum Status { active, disactive }

    /** First image out of the JSON-ish `images` text column, or null. */
    @Transient
    public String getFirstImage() {
        if (images == null || images.isBlank()) return null;
        String stripped = images.replaceAll("[\\[\\]\"\\\\]", "");
        String[] parts = stripped.split(",");
        return parts.length > 0 && !parts[0].isBlank() ? parts[0].trim() : null;
    }

    /** All images out of the JSON-ish `images` text column. */
    @Transient
    public java.util.List<String> getImageList() {
        if (images == null || images.isBlank()) return java.util.List.of();
        String stripped = images.replaceAll("[\\[\\]\"\\\\]", "");
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String part : stripped.split(",")) {
            if (!part.isBlank()) out.add(part.trim());
        }
        return out;
    }

    /** `property` is a small JSON array like [{"name":"حمام","number":"3"}, ...]. */
    @Transient
    public java.util.List<java.util.Map<String, String>> getRoomBreakdown() {
        if (property == null || property.isBlank()) return java.util.List.of();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(property,
                    mapper.getTypeFactory().constructCollectionType(java.util.List.class, java.util.Map.class));
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    @Transient
    public boolean isHasVirtualTour() {
        return arPath != null && !arPath.isBlank();
    }

    /** end_date / creation_date are stored as dd/MM/yyyy text, not real DATE columns. */
    @Transient
    public boolean isLicenseExpired() {
        if (endDate == null || endDate.isBlank()) return false;
        try {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            java.time.LocalDate end = java.time.LocalDate.parse(endDate.trim(), fmt);
            return end.isBefore(java.time.LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }
}
