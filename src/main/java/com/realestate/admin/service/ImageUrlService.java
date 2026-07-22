package com.realestate.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds display URLs for media stored in the `abaad-media-assets` R2
 * bucket. The bucket's public read URL isn't secret (it's a read-only
 * public endpoint), so unlike the R2 API credentials, it's safe to store
 * and edit live from Settings -> Media Storage - no restart needed to
 * change it.
 */
@Component("imageUrlService")
@RequiredArgsConstructor
public class ImageUrlService {

    @Value("${app.r2.public-base-url:}")
    private String fallbackBaseUrl;

    private final SettingsService settingsService;

    public String estateImage(String filename) { return build("estate", filename); }
    public String offerImage(String filename) { return build("offers", filename); }
    public String categoryImage(String filename) { return build("categories", filename); }
    public String profileImage(String filename) { return build("profile", filename); }
    public String providerImage(String filename) { return build("service-providers", filename); }
    public String video(String filename) { return build("videos", filename); }

    /** Folder base URL (trailing slash included) - handy for JS-driven pages like the map view. */
    public String estateBase() { return folderBase("estate"); }
    public String offerBase() { return folderBase("offers"); }

    private String folderBase(String folder) {
        String base = publicBaseUrl();
        if (base == null) return "/uploads/";
        return base + "/" + folder + "/";
    }

    private String build(String folder, String filename) {
        if (filename == null || filename.isBlank()) return null;
        String base = publicBaseUrl();
        if (base == null) return "/uploads/" + filename;
        return base + "/" + folder + "/" + filename;
    }

    private String publicBaseUrl() {
        String configured = settingsService.get("r2_public_url", fallbackBaseUrl);
        if (configured == null || configured.isBlank()) return null;
        return configured.replaceAll("/+$", "");
    }
}
