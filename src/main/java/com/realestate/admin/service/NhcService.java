package com.realestate.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Talks to the NHC "AdvertisementValidator" API to pull verified property
 * data straight from the government registry, given just the ad license
 * number + advertiser ID. Credentials come from business_settings (Settings
 * -> nhc_client_id / nhc_client_secret), same live-editable pattern as R2 -
 * never hardcoded here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NhcService {

    private static final String URL = "https://integration-gw.nhc.sa/nhc/prod/v2/brokerage/AdvertisementValidator";

    private final SettingsService settingsService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record LookupResult(boolean success, JsonNode advertisement, String error) {
    }

    public boolean isConfigured() {
        return !settingsService.get("nhc_client_id", "").isBlank()
                && !settingsService.get("nhc_client_secret", "").isBlank();
    }

    public LookupResult lookup(String licenseNumber, String advertiserId, String idType) {
        if (!isConfigured()) {
            return new LookupResult(false, null, "not_configured");
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(URL)
                    .queryParam("adLicenseNumber", licenseNumber)
                    .queryParam("advertiserId", advertiserId)
                    .queryParam("idType", idType)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-IBM-Client-Id", settingsService.get("nhc_client_id", ""));
            headers.set("X-IBM-Client-Secret", settingsService.get("nhc_client_secret", ""));
            headers.set("RefId", "0");

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return new LookupResult(false, null, "http_" + response.getStatusCode().value());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode ad = root.path("Body").path("result").path("advertisement");
            if (ad.isMissingNode() || ad.isNull()) {
                return new LookupResult(false, null, "not_found");
            }
            return new LookupResult(true, ad, null);
        } catch (Exception e) {
            log.error("NHC lookup failed", e);
            return new LookupResult(false, null, e.getMessage());
        }
    }
}
