package com.realestate.admin.controller.web;

import com.realestate.admin.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public, unauthenticated pages meant to be reachable from outside the
 * admin (privacy policy, terms, etc). Content comes straight from
 * business_settings via SettingsService, so it stays editable from
 * Settings -> Legal Content without touching code or redeploying.
 */
@Controller
@RequiredArgsConstructor
public class PublicPageController {

    private final SettingsService settingsService;

    @GetMapping("/privacy-policy")
    public String privacyPolicy(Model model) {
        model.addAttribute("companyName", settingsService.get("company_name", "أبعاد"));
        model.addAttribute("contentAr", settingsService.get("privacy_policy_web_ar", ""));
        model.addAttribute("contentEn", settingsService.get("privacy_policy_web", ""));
        model.addAttribute("email", settingsService.get("company_email", ""));
        model.addAttribute("phone", settingsService.get("company_phone", ""));
        return "public/privacy-policy";
    }

    @GetMapping("/terms-and-conditions")
    public String terms(Model model) {
        model.addAttribute("companyName", settingsService.get("company_name", "أبعاد"));
        model.addAttribute("contentAr", settingsService.get("terms_condition", ""));
        model.addAttribute("contentEn", settingsService.get("terms_and_conditions", ""));
        model.addAttribute("email", settingsService.get("company_email", ""));
        model.addAttribute("phone", settingsService.get("company_phone", ""));
        return "public/terms";
    }
}
