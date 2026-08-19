package com.realestate.admin.controller.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.admin.dto.BrandColors;
import com.realestate.admin.dto.StoreLink;
import com.realestate.admin.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("companyName", settingsService.get("company_name", ""));
        model.addAttribute("companyPhone", settingsService.get("company_phone", ""));
        model.addAttribute("companyEmail", settingsService.get("company_email", ""));
        model.addAttribute("address", settingsService.get("address", ""));
        model.addAttribute("copyrightText", settingsService.get("company_copyright_text", ""));

        model.addAttribute("colors", readColors());

        model.addAttribute("googleStore", readStoreLink("download_app_google_stroe"));
        model.addAttribute("appleStore", readStoreLink("download_app_apple_stroe"));

        model.addAttribute("fcmTopic", settingsService.get("fcm_topic", ""));
        model.addAttribute("fcmProjectId", settingsService.get("fcm_project_id", ""));
        model.addAttribute("pushNotificationKey", settingsService.get("push_notification_key", ""));

        model.addAttribute("agentRegistration", "1".equals(settingsService.get("agent_registration", "0")));
        model.addAttribute("sellerRegistration", "1".equals(settingsService.get("seller_registration", "0")));
        model.addAttribute("phoneVerification", "1".equals(settingsService.get("phone_verification", "0")));
        model.addAttribute("emailVerification", "1".equals(settingsService.get("email_verification", "0")));

        model.addAttribute("mapApiKey", settingsService.get("map_api_key", "AIzaSyAwM15LYUky7qqVuXdBQc9zavA39y487jQ"));
        model.addAttribute("countryCode", settingsService.get("country_code", ""));
        model.addAttribute("paginationLimit", settingsService.get("pagination_limit", "10"));
        model.addAttribute("timezone", settingsService.get("timezone", "UTC"));

        model.addAttribute("r2PublicUrl", settingsService.get("r2_public_url", ""));
        model.addAttribute("r2AccountId", settingsService.get("r2_account_id", "c33f368b51e236e8892b759dab9c1549"));
        model.addAttribute("r2Bucket", settingsService.get("r2_bucket", "abaad-media-assets"));
        model.addAttribute("r2AccessKeyId", settingsService.get("r2_access_key_id", ""));
        model.addAttribute("r2HasSecret", !settingsService.get("r2_secret_access_key", "").isBlank());

        model.addAttribute("aboutUs", settingsService.get("about_us", ""));
        model.addAttribute("termsCondition", settingsService.get("terms_condition", ""));
        model.addAttribute("privacyPolicy", settingsService.get("privacy_policy", ""));
        model.addAttribute("aboutUsWeb", settingsService.get("about_us_web", ""));
        model.addAttribute("aboutUsWebAr", settingsService.get("about_us_web_ar", ""));
        model.addAttribute("privacyPolicyWeb", settingsService.get("privacy_policy_web", ""));
        model.addAttribute("privacyPolicyWebAr", settingsService.get("privacy_policy_web_ar", ""));
        model.addAttribute("termsAndConditions", settingsService.get("terms_and_conditions", ""));

        model.addAttribute("webLogo", settingsService.get("company_web_logo", ""));
        model.addAttribute("mobLogo", settingsService.get("company_mobile_logo", ""));
        model.addAttribute("favIcon", settingsService.get("company_fav_icon", ""));
        model.addAttribute("footerLogo", settingsService.get("company_footer_logo", ""));

        model.addAttribute("walletStatus", "1".equals(settingsService.get("wallet_status", "0")));
        model.addAttribute("loyaltyPointStatus", "1".equals(settingsService.get("loyalty_point_status", "0")));
        model.addAttribute("guestCheckoutStatus", "1".equals(settingsService.get("guest_checkout", "0")));

        // maintenance_mode uses literal "true"/"false" strings (matches json_decode()
        // on the Laravel side turning them into real booleans) - NOT "1"/"0" like the
        // other toggles above.
        model.addAttribute("maintenanceMode", "true".equals(settingsService.get("maintenance_mode", "false")));

        model.addAttribute("appMinVersionAndroid", settingsService.get("app_min_version_android", "1.0"));
        model.addAttribute("appMinVersionIos", settingsService.get("app_min_version_ios", "1.0"));

        model.addAttribute("nhcClientId", settingsService.get("nhc_client_id", ""));
        model.addAttribute("nhcHasSecret", !settingsService.get("nhc_client_secret", "").isBlank());

        model.addAttribute("activePage", "settings");

        model.addAttribute("firebaseServiceAccountJson", settingsService.get("firebase_service_account_json", ""));
model.addAttribute("firebaseHasCredentials", !settingsService.get("firebase_service_account_json", "").isBlank());

        String fbJson = settingsService.get("firebase_service_account_json", "");
        if (!fbJson.isBlank()) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(fbJson);
                model.addAttribute("firebaseProjectId", node.path("project_id").asText(""));
                model.addAttribute("firebaseClientEmail", node.path("client_email").asText(""));
            } catch (Exception ignored) {
            }
        }
model.addAttribute("firebaseEnabled", "1".equals(settingsService.get("firebase_enabled", "0")));
        return "settings";
    }

    @PostMapping("/settings/save")
    public String save(@RequestParam Map<String, String> form, RedirectAttributes redirectAttributes) {

        settingsService.set("company_name", form.get("companyName"));
        settingsService.set("company_phone", form.get("companyPhone"));
        settingsService.set("company_email", form.get("companyEmail"));
        settingsService.set("address", form.get("address"));
        settingsService.set("company_copyright_text", form.get("copyrightText"));

        settingsService.set("colors", writeColors(form.get("colorPrimary"), form.get("colorSecondary")));

        settingsService.set("download_app_google_stroe",
                writeStoreLink(form.containsKey("googleStoreEnabled"), form.get("googleStoreLink")));
        settingsService.set("download_app_apple_stroe",
                writeStoreLink(form.containsKey("appleStoreEnabled"), form.get("appleStoreLink")));

        settingsService.set("fcm_topic", form.get("fcmTopic"));
        settingsService.set("fcm_project_id", form.get("fcmProjectId"));
        if (form.get("pushNotificationKey") != null && !form.get("pushNotificationKey").isBlank()) {
            settingsService.set("push_notification_key", form.get("pushNotificationKey"));
        }

        settingsService.set("agent_registration", form.containsKey("agentRegistration") ? "1" : "0");
        settingsService.set("seller_registration", form.containsKey("sellerRegistration") ? "1" : "0");
        settingsService.set("phone_verification", form.containsKey("phoneVerification") ? "1" : "0");
        settingsService.set("email_verification", form.containsKey("emailVerification") ? "1" : "0");

        settingsService.set("map_api_key", form.get("mapApiKey"));
        settingsService.set("country_code", form.get("countryCode"));
        settingsService.set("pagination_limit", form.get("paginationLimit"));
        settingsService.set("timezone", form.get("timezone"));

        settingsService.set("r2_public_url", form.get("r2PublicUrl"));
        settingsService.set("r2_account_id", form.get("r2AccountId"));
        settingsService.set("r2_bucket", form.get("r2Bucket"));
        settingsService.set("r2_access_key_id", form.get("r2AccessKeyId"));
        if (form.get("r2SecretAccessKey") != null && !form.get("r2SecretAccessKey").isBlank()) {
            settingsService.set("r2_secret_access_key", form.get("r2SecretAccessKey"));
        }

        settingsService.set("about_us", form.get("aboutUs"));
        settingsService.set("terms_condition", form.get("termsCondition"));
        settingsService.set("privacy_policy", form.get("privacyPolicy"));
        settingsService.set("about_us_web", form.get("aboutUsWeb"));
        settingsService.set("about_us_web_ar", form.get("aboutUsWebAr"));
        settingsService.set("privacy_policy_web", form.get("privacyPolicyWeb"));
        settingsService.set("privacy_policy_web_ar", form.get("privacyPolicyWebAr"));
        settingsService.set("terms_and_conditions", form.get("termsAndConditions"));

        settingsService.set("company_web_logo", form.get("webLogo"));
        settingsService.set("company_mobile_logo", form.get("mobLogo"));
        settingsService.set("company_fav_icon", form.get("favIcon"));
        settingsService.set("company_footer_logo", form.get("footerLogo"));

        settingsService.set("wallet_status", form.containsKey("walletStatus") ? "1" : "0");
        settingsService.set("loyalty_point_status", form.containsKey("loyaltyPointStatus") ? "1" : "0");
        settingsService.set("guest_checkout", form.containsKey("guestCheckoutStatus") ? "1" : "0");

        // maintenance_mode: literal "true"/"false" strings - see note in settings() above
        settingsService.set("maintenance_mode", form.containsKey("maintenanceMode") ? "true" : "false");

        settingsService.set("app_min_version_android", form.get("appMinVersionAndroid"));
        settingsService.set("app_min_version_ios", form.get("appMinVersionIos"));


        if (form.get("firebaseServiceAccountJson") != null && !form.get("firebaseServiceAccountJson").isBlank()) {
    settingsService.set("firebase_service_account_json", form.get("firebaseServiceAccountJson"));
}
settingsService.set("firebase_enabled", form.containsKey("firebaseEnabled") ? "1" : "0");

        settingsService.set("nhc_client_id", form.get("nhcClientId"));
        if (form.get("nhcClientSecret") != null && !form.get("nhcClientSecret").isBlank()) {
            settingsService.set("nhc_client_secret", form.get("nhcClientSecret"));
        }

        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/settings";
    }

    private BrandColors readColors() {
        try {
            Map<String, String> raw = objectMapper.readValue(
                    settingsService.get("colors", "{}"), Map.class);
            return new BrandColors(raw.getOrDefault("primary", "#1b4b7c"), raw.getOrDefault("secondary", "#000000"));
        } catch (Exception e) {
            return new BrandColors("#1b4b7c", "#000000");
        }
    }

    private String writeColors(String primary, String secondary) {
        Map<String, String> map = new HashMap<>();
        map.put("primary", primary != null ? primary : "#1b4b7c");
        map.put("secondary", secondary != null ? secondary : "#000000");
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private StoreLink readStoreLink(String type) {
        try {
            Map<String, Object> raw = objectMapper.readValue(settingsService.get(type, "{}"), Map.class);
            boolean enabled = "1".equals(String.valueOf(raw.get("status")));
            String link = String.valueOf(raw.getOrDefault("link", ""));
            return new StoreLink(enabled, "null".equals(link) ? "" : link);
        } catch (Exception e) {
            return new StoreLink(false, "");
        }
    }

    private String writeStoreLink(boolean enabled, String link) {
        Map<String, String> map = new HashMap<>();
        map.put("status", enabled ? "1" : "0");
        map.put("link", link != null ? link : "");
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}