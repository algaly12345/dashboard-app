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

@Service
@RequiredArgsConstructor
@Slf4j
public class NhcService {

    private static final String URL = "https://integration-gw.nhc.sa/nhc/prod/v2/brokerage/AdvertisementValidator";

    private final SettingsService settingsService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** rawResponse is the full JSON body NHC returned, always included when we got one -
     *  handy for diagnosing/mapping fields that aren't wired up yet. */
    public record LookupResult(boolean success, JsonNode advertisement, String rawResponse, String error) {
    }

    private static final String VALIDATE_URL = "https://app.abaadapp.sa/api/v1/banners/advertisement/validate";

    /** Fetches just the responsible-employee name/phone for an existing estate,
     *  via the Laravel wrapper's lighter validation endpoint - separate from
     *  the full NHC lookup() above (different URL, different response shape). */
    public LookupResult fetchResponsibleEmployee(String licenseNumber, String advertiserId, String idType) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(VALIDATE_URL)
                    .queryParam("adLicenseNumber", licenseNumber)
                    .queryParam("advertiserId", advertiserId)
                    .queryParam("idType", idType)
                    .toUriString();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(settingsService.get("laravel_admin_token", ""));
            headers.set("Accept", "application/json");
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return new LookupResult(false, null, response.getBody(), "http_" + response.getStatusCode().value());
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode ad = root.path("data").path("advertisement");
            if (ad.isMissingNode() || ad.isNull()) {
                return new LookupResult(false, null, response.getBody(), "not_found");
            }
            return new LookupResult(true, ad, response.getBody(), null);
        } catch (Exception e) {
            log.error("Responsible-employee fetch failed", e);
            return new LookupResult(false, null, null, e.getMessage());
        }
    }

    public boolean isConfigured() {
        return !settingsService.get("nhc_client_id", "").isBlank()
                && !settingsService.get("nhc_client_secret", "").isBlank();
    }

    public LookupResult lookup(String licenseNumber, String advertiserId, String idType) {
        if (!isConfigured()) {
            return new LookupResult(false, null, null, "not_configured");
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
                return new LookupResult(false, null, response.getBody(), "http_" + response.getStatusCode().value());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode ad = root.path("Body").path("result").path("advertisement");
            if (ad.isMissingNode() || ad.isNull()) {
                return new LookupResult(false, null, response.getBody(), "not_found");
            }
            return new LookupResult(true, ad, response.getBody(), null);
        } catch (Exception e) {
            log.error("NHC lookup failed", e);
            return new LookupResult(false, null, null, e.getMessage());
        }
    }
}