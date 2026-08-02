package com.realestate.admin.controller.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.admin.entity.Category;
import com.realestate.admin.entity.Estate;
import com.realestate.admin.repository.CategoryRepository;
import com.realestate.admin.repository.EstateRepository;
import com.realestate.admin.repository.ZoneRepository;
import com.realestate.admin.service.NhcService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

/**
 * Admin-driven estate creation via the NHC registry lookup.
 *
 * Flow: /estates/new (form: license number, advertiser/national ID number,
 * ID type, zone) -> POST /estates/lookup calls NHC and, on success, renders
 * a PREVIEW (nothing saved yet) with every field NHC returned shown as
 * read-only text plus mirrored into hidden inputs -> POST /estates/confirm
 * takes those hidden inputs and actually creates the row. NHC is only ever
 * called once per attempt - confirm just persists what was already fetched.
 */
@Controller
@RequiredArgsConstructor
public class EstateCreateController {

    private final NhcService nhcService;
    private final EstateRepository estateRepository;
    private final ZoneRepository zoneRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/estates/new")
    public String newForm(Model model) {
        model.addAttribute("zones", zoneRepository.findAll());
        model.addAttribute("nhcConfigured", nhcService.isConfigured());
        model.addAttribute("activePage", "estates");
        return "estate-new";
    }

    @PostMapping("/estates/lookup")
    public String lookup(@RequestParam String licenseNumber,
                          @RequestParam String advertiserNumber,
                          @RequestParam String idType,
                          @RequestParam(required = false) String zoneId,
                          @RequestParam(required = false) String userId,
                          Model model,
                          RedirectAttributes redirectAttributes) {

        NhcService.LookupResult result = nhcService.lookup(licenseNumber, advertiserNumber, idType);

        if (!result.success()) {
            redirectAttributes.addFlashAttribute("lookupError", result.error());
            redirectAttributes.addFlashAttribute("licenseNumber", licenseNumber);
            redirectAttributes.addFlashAttribute("advertiserNumber", advertiserNumber);
            return "redirect:/estates/new";
        }

        JsonNode ad = result.advertisement();

        // Everything below is what NHC returned - shown for review, nothing
        // touches the database yet.
        model.addAttribute("zoneId", zoneId);
        model.addAttribute("userId", userId);
        model.addAttribute("licenseNumber", licenseNumber);

        model.addAttribute("deedNumber", text(ad, "deedNumber"));
        model.addAttribute("advertiserName", text(ad, "advertiserName"));
        model.addAttribute("phoneNumber", text(ad, "phoneNumber"));
        model.addAttribute("propertyType", text(ad, "propertyType"));
        model.addAttribute("advertisementType", text(ad, "advertisementType"));
        model.addAttribute("postalCode", text(ad, "location", "postalCode"));
        model.addAttribute("city", text(ad, "location", "city"));
        model.addAttribute("districts", text(ad, "location", "district"));
        model.addAttribute("latitude", text(ad, "location", "latitude"));
        model.addAttribute("longitude", text(ad, "location", "longitude"));
        model.addAttribute("creationDate", text(ad, "creationDate"));
        model.addAttribute("endDate", text(ad, "endDate"));
        model.addAttribute("totalPrice", text(ad, "landTotalPrice"));
        model.addAttribute("propertyFace", text(ad, "propertyFace"));
        model.addAttribute("adLicenseNumber", text(ad, "adLicenseNumber"));
        model.addAttribute("landNumber", text(ad, "landNumber"));
        model.addAttribute("titleDeedTypeName", text(ad, "titleDeedTypeName"));
        model.addAttribute("adLicenseUrl", text(ad, "adLicenseUrl"));
        model.addAttribute("numberOfRooms", text(ad, "numberOfRooms"));
        model.addAttribute("locationDescriptionOnMOJDeed", text(ad, "locationDescriptionOnMOJDeed"));
        model.addAttribute("guaranteesAndTheirDuration", text(ad, "guaranteesAndTheirDuration"));
        model.addAttribute("obligationsOnTheProperty", text(ad, "obligationsOnTheProperty"));
        model.addAttribute("brokerageAndMarketingLicenseNumber", text(ad, "brokerageAndMarketingLicenseNumber"));
        model.addAttribute("propertyUtilities", ad.has("propertyUtilities") ? ad.get("propertyUtilities").toString() : "[]");

        // Same swap as the existing Laravel implementation - mainLandUseTypeName
        // gets propertyUsages[0], propertyUsages gets mainLandUseTypeName.
        // Kept identical on purpose for behavior parity; flag if you want it fixed.
        String propertyUsagesFirst = ad.path("propertyUsages").isArray() && ad.path("propertyUsages").size() > 0
                ? ad.path("propertyUsages").get(0).asText() : null;
        model.addAttribute("mainLandUseTypeName", propertyUsagesFirst);
        model.addAttribute("propertyUsages", text(ad, "mainLandUseTypeName"));

        String categoryName = text(ad, "propertyType");
        model.addAttribute("categoryName", categoryName);

        model.addAttribute("activePage", "estates");
        return "estate-preview";
    }

    @PostMapping("/estates/confirm")
    public String confirm(@RequestParam java.util.Map<String, String> form,
                           RedirectAttributes redirectAttributes) {

        Estate estate = new Estate();
        estate.setStatus(Estate.Status.active);
        estate.setZoneId(parseLongOrNull(form.get("zoneId")));
        estate.setUserId(parseLongOrNull(form.get("userId")));
        estate.setImages("[]");
        estate.setCreatedAt(LocalDateTime.now());
        estate.setUpdatedAt(LocalDateTime.now());

        estate.setLicenseNumber(form.get("licenseNumber"));
        estate.setDeedNumber(form.get("deedNumber"));
        estate.setAdvertiserName(form.get("advertiserName"));
        estate.setPhoneNumber(form.get("phoneNumber"));
        estate.setPropertyType(form.get("propertyType"));
        estate.setAdvertisementType(form.get("advertisementType"));
        estate.setPostalCode(parseIntOrNull(form.get("postalCode")));
        estate.setCity(form.get("city"));
        estate.setDistricts(form.get("districts"));
        estate.setLatitude(form.get("latitude"));
        estate.setLongitude(form.get("longitude"));
        estate.setCreationDate(form.get("creationDate"));
        estate.setEndDate(form.get("endDate"));
        estate.setTotalPrice(form.get("totalPrice"));
        estate.setPropertyFace(form.get("propertyFace"));
        estate.setAdLicenseNumber(form.get("adLicenseNumber"));
        estate.setLandNumber(form.get("landNumber"));
        estate.setTitleDeedTypeName(form.get("titleDeedTypeName"));
        estate.setAdLicenseUrl(form.get("adLicenseUrl"));
        estate.setNumberOfRooms(form.get("numberOfRooms"));
        estate.setLocationDescriptionOnMOJDeed(form.get("locationDescriptionOnMOJDeed"));
        estate.setGuaranteesAndTheirDuration(form.get("guaranteesAndTheirDuration"));
        estate.setObligationsOnTheProperty(form.get("obligationsOnTheProperty"));
        estate.setBrokerageAndMarketingLicenseNumber(form.get("brokerageAndMarketingLicenseNumber"));
        estate.setPropertyUtilities(form.get("propertyUtilities"));
        estate.setMainLandUseTypeName(form.get("mainLandUseTypeName"));
        estate.setPropertyUsages(form.get("propertyUsages"));

        String categoryName = form.get("categoryName");
        estate.setCategoryName(categoryName);
        if (categoryName != null) {
            Category category = categoryRepository.findAll().stream()
                    .filter(c -> categoryName.equals(c.getNameAr()) || categoryName.equals(c.getName()))
                    .findFirst().orElse(null);
            if (category != null) {
                estate.setCategoryId(category.getId() != null ? category.getId().intValue() : null);
            }
        }

        estateRepository.save(estate);

        redirectAttributes.addFlashAttribute("created", true);
        return "redirect:/estates/" + estate.getId() + "/edit";
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private String text(JsonNode node, String parent, String field) {
        JsonNode v = node.path(parent).path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private Long parseLongOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}