package com.realestate.admin.dto;

/** What the estates-list "advertiser" modal needs - a resolved image URL
 *  (not just a bare filename) plus quick stats, built once per page load. */
public record AdvertiserInfo(
        Long id,
        String name,
        String phone,
        String email,
        String unifiedNumber,
        Integer advertiserNo,
        String falLicenseNumber,
        String imageUrl,
        String youtube,
        String snapchat,
        String instagram,
        String website,
        String tiktok,
        String twitter,
        long estateCount
) {
}
