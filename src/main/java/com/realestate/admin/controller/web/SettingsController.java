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
        // ---- Company info ----
        model.addAttribute("companyName", settingsService.get("company_name", ""));
        model.addAttribute("companyPhone", settingsService.get("company_phone", ""));
        model.addAttribute("companyEmail", settingsService.get("company_email", ""));
        model.addAttribute("address", settingsService.get("address", ""));
        model.addAttribute("copyrightText", settingsService.get("company_copyright_text", ""));

        // ---- Brand colors ----
        model.addAttribute("colors", readColors());

        // ---- App store links ----
        model.addAttribute("googleStore", readStoreLink("download_app_google_stroe"));
        model.addAttribute("appleStore", readStoreLink("download_app_apple_stroe"));

        // ---- Push notifications (FCM legacy settings) ----
        model.addAttribute("fcmTopic", settingsService.get("fcm_topic", ""));
        model.addAttribute("fcmProjectId", settingsService.get("fcm_project_id", ""));
        model.addAttribute("pushNotificationKey", settingsService.get("push_notification_key", ""));

        // ---- Registration & verification toggles ----
        model.addAttribute("agentRegistration", "1".equals(settingsService.get("agent_registration", "0")));
        model.addAttribute("sellerRegistration", "1".equals(settingsService.get("seller_registration", "0")));
        model.addAttribute("phoneVerification", "1".equals(settingsService.get("phone_verification", "0")));
        model.addAttribute("emailVerification", "1".equals(settingsService.get("email_verification", "0")));

        // ---- Map & regional ----
        model.addAttribute("mapApiKey", settingsService.get("map_api_key", "AIzaSyAwM15LYUky7qqVuXdBQc9zavA39y487jQ"));
        model.addAttribute("countryCode", settingsService.get("country_code", ""));
        model.addAttribute("paginationLimit", settingsService.get("pagination_limit", "10"));
        model.addAttribute("timezone", settingsService.get("timezone", "UTC"));

        // ---- Media storage (Cloudflare R2) ----
        model.addAttribute("r2PublicUrl", settingsService.get("r2_public_url", ""));

        // ---- Legal / content pages ----
        model.addAttribute("aboutUs", settingsService.get("about_us", ""));
        model.addAttribute("termsCondition", settingsService.get("terms_condition", ""));
        model.addAttribute("privacyPolicy", settingsService.get("privacy_policy", ""));

        model.addAttribute("activePage", "settings");
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

        settingsService.set("about_us", form.get("aboutUs"));
        settingsService.set("terms_condition", form.get("termsCondition"));
        settingsService.set("privacy_policy", form.get("privacyPolicy"));

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
