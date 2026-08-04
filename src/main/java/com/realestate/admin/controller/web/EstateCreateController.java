package com.realestate.admin.controller.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.admin.entity.Agent;
import com.realestate.admin.entity.AppUser;
import com.realestate.admin.entity.Category;
import com.realestate.admin.entity.Estate;
import com.realestate.admin.repository.AgentRepository;
import com.realestate.admin.repository.AppUserRepository;
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
import java.util.Optional;

/**
 * Admin-driven estate creation via the NHC registry lookup (through the
 * Laravel wrapper endpoint, which already holds the NHC credentials).
 *
 * Flow: /estates/new (form: license number, advertiser number, ID type:
 * 1=individual/فرد -> matched against agents.identity, 2=entity/منشأة ->
 * matched against agents.unified_number, zone) -> POST /estates/lookup
 * calls the wrapper and, on success, renders a PREVIEW (nothing saved yet)
 * -> POST /estates/confirm:
 *   - looks the advertiser up in `agents` by identity or unified_number
 *   - if found: the estate is just linked to that agent's existing user_id,
 *     nothing new is created
 *   - if not found: creates a new `users` row AND a new `agents` row for
 *     them, then links the estate to the new user
 */
@Controller
@RequiredArgsConstructor
public class EstateCreateController {

    private final NhcService nhcService;
    private final EstateRepository estateRepository;
    private final ZoneRepository zoneRepository;
    private final CategoryRepository categoryRepository;
    private final AppUserRepository appUserRepository;
    private final AgentRepository agentRepository;
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
                          Model model,
                          RedirectAttributes redirectAttributes) {

        NhcService.LookupResult result = nhcService.lookup(licenseNumber, advertiserNumber, idType);

        if (!result.success()) {
            redirectAttributes.addFlashAttribute("lookupError", result.error());
            redirectAttributes.addFlashAttribute("rawJson", result.rawResponse());
            redirectAttributes.addFlashAttribute("licenseNumber", licenseNumber);
            redirectAttributes.addFlashAttribute("advertiserNumber", advertiserNumber);
            return "redirect:/estates/new";
        }

        JsonNode ad = result.advertisement();
        model.addAttribute("rawJson", result.rawResponse());

        model.addAttribute("licenseNumber", licenseNumber);
        model.addAttribute("advertiserNumber", advertiserNumber);
        model.addAttribute("idType", idType);

        // ---- Zones - not returned by NHC (it's an internal platform concept,
        //      not an administrative region), but we suggest the closest match
        //      by city name so the admin isn't picking from scratch. ----
        java.util.List<com.realestate.admin.entity.Zone> allZones = zoneRepository.findAll();
        String cityText = text(ad, "location", "city");
        Long suggestedZoneId = allZones.stream()
                .filter(z -> cityText != null && z.getNameAr() != null && z.getNameAr().contains(cityText))
                .map(com.realestate.admin.entity.Zone::getId)
                .findFirst().orElse(null);
        model.addAttribute("zones", allZones);
        model.addAttribute("suggestedZoneId", suggestedZoneId);

        // ---- People / identity ----
        model.addAttribute("advertiserName", text(ad, "advertiserName"));
        model.addAttribute("phoneNumber", text(ad, "phoneNumber"));
        model.addAttribute("responsibleEmployeeName", text(ad, "responsibleEmployeeName"));
        model.addAttribute("responsibleEmployeePhoneNumber", text(ad, "responsibleEmployeePhoneNumber"));

        // ---- Property basics ----
        model.addAttribute("propertyType", text(ad, "propertyType"));
        model.addAttribute("propertyAge", text(ad, "propertyAge"));
        model.addAttribute("advertisementType", text(ad, "advertisementType"));
        model.addAttribute("propertyArea", text(ad, "propertyArea"));
        model.addAttribute("propertyPrice", text(ad, "propertyPrice"));
        model.addAttribute("landTotalPrice", text(ad, "landTotalPrice"));
        model.addAttribute("numberOfRooms", text(ad, "numberOfRooms"));
        model.addAttribute("propertyFace", text(ad, "propertyFace"));
        model.addAttribute("streetWidth", text(ad, "streetWidth"));

        // ---- Location ----
        model.addAttribute("region", text(ad, "location", "region"));
        model.addAttribute("city", text(ad, "location", "city"));
        model.addAttribute("districts", text(ad, "location", "district"));
        model.addAttribute("street", text(ad, "location", "street"));
        model.addAttribute("postalCode", text(ad, "location", "postalCode"));
        model.addAttribute("buildingNumber", text(ad, "location", "buildingNumber"));
        model.addAttribute("additionalNumber", text(ad, "location", "additionalNumber"));
        model.addAttribute("latitude", text(ad, "location", "latitude"));
        model.addAttribute("longitude", text(ad, "location", "longitude"));

        // ---- Legal / deed ----
        model.addAttribute("deedNumber", text(ad, "deedNumber"));
        model.addAttribute("titleDeedTypeName", text(ad, "titleDeedTypeName"));
        model.addAttribute("landNumber", text(ad, "landNumber"));
        model.addAttribute("planNumber", text(ad, "planNumber"));
        model.addAttribute("mainLandUseTypeName", text(ad, "mainLandUseTypeName"));
        model.addAttribute("redZoneTypeName", text(ad, "redZoneTypeName"));
        model.addAttribute("creationDate", text(ad, "creationDate"));
        model.addAttribute("endDate", text(ad, "endDate"));
        model.addAttribute("adLicenseNumber", text(ad, "adLicenseNumber"));
        model.addAttribute("brokerageAndMarketingLicenseNumber", text(ad, "brokerageAndMarketingLicenseNumber"));
        model.addAttribute("adLicenseUrl", text(ad, "adLicenseUrl"));
        model.addAttribute("adSource", text(ad, "adSource"));
        model.addAttribute("locationDescriptionOnMOJDeed", text(ad, "locationDescriptionOnMOJDeed"));
        model.addAttribute("obligationsOnTheProperty", text(ad, "obligationsOnTheProperty"));
        model.addAttribute("guaranteesAndTheirDuration", text(ad, "guaranteesAndTheirDuration"));
        model.addAttribute("notes", text(ad, "notes"));

        // ---- Flags (constraints on the property) ----
        model.addAttribute("isConstrained", ad.path("isConstrained").asBoolean(false));
        model.addAttribute("isPawned", ad.path("isPawned").asBoolean(false));
        model.addAttribute("isHalted", ad.path("isHalted").asBoolean(false));
        model.addAttribute("isTestment", ad.path("isTestment").asBoolean(false));

        // ---- Utilities / usages ----
        model.addAttribute("propertyUtilities", ad.has("propertyUtilities") ? ad.get("propertyUtilities").toString() : "[]");
        String propertyUsagesFirst = ad.path("propertyUsages").isArray() && ad.path("propertyUsages").size() > 0
                ? ad.path("propertyUsages").get(0).asText() : null;
        model.addAttribute("propertyUsages", propertyUsagesFirst);

        // ---- Borders ----
        model.addAttribute("northLimit", borderText(ad, "north"));
        model.addAttribute("eastLimit", borderText(ad, "east"));
        model.addAttribute("westLimit", borderText(ad, "west"));
        model.addAttribute("southLimit", borderText(ad, "south"));

        String categoryName = text(ad, "propertyType");
        model.addAttribute("categoryName", categoryName);

        // Preview whether this advertiser already has an agent record - matched
        // by identity (individual) or unified_number (entity), NOT by phone.
        Optional<Agent> existing = "2".equals(idType)
                ? agentRepository.findByUnifiedNumber(advertiserNumber)
                : agentRepository.findByIdentity(advertiserNumber);
        model.addAttribute("existingUserFound", existing.isPresent());
        model.addAttribute("existingUserName", existing.map(Agent::getName).orElse(null));

        model.addAttribute("activePage", "estates");
        return "estate-preview";
    }

    @PostMapping("/estates/confirm")
    public String confirm(@RequestParam java.util.Map<String, String> form,
                           RedirectAttributes redirectAttributes) {

        Long zoneId = parseLongOrNull(form.get("zoneId"));
        String phone = form.get("phoneNumber");
        String advertiserName = form.get("advertiserName");
        String falLicense = form.get("brokerageAndMarketingLicenseNumber");
        String advertiserNumber = form.get("advertiserNumber");
        String idType = form.get("idType");
        boolean isEntity = "2".equals(idType);

        // ---- Look the advertiser up in `agents` first - if they exist, just
        //      reuse their user_id, don't create anything new. ----
        Optional<Agent> existingAgent = isEntity
                ? agentRepository.findByUnifiedNumber(advertiserNumber)
                : agentRepository.findByIdentity(advertiserNumber);

        Long resolvedUserId;
        if (existingAgent.isPresent()) {
            resolvedUserId = existingAgent.get().getUserId();
        } else {
            // ---- Genuinely new advertiser: create both `users` and `agents` ----
            AppUser user = new AppUser();
            user.setId(appUserRepository.findMaxId() + 1);
            user.setName(advertiserName != null ? advertiserName : advertiserNumber);
            user.setPhone(phone != null ? phone : "");
            user.setIsActive(AppUser.Status.active);
            user.setUserType("provider");
            user.setZoneId(zoneId);
            user.setFalLicenseNumber(falLicense);
            user.setWalletBalance(java.math.BigDecimal.ZERO);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            appUserRepository.save(user);
            resolvedUserId = user.getId();

            Agent agent = new Agent();
            agent.setId(agentRepository.findMaxId() + 1);
            agent.setName(advertiserName != null ? advertiserName : advertiserNumber);
            agent.setPhone(phone);
            if (isEntity) {
                agent.setUnifiedNumber(advertiserNumber);
            } else {
                agent.setIdentity(advertiserNumber);
            }
            agent.setAdvertiserNo(advertiserNumber);
            agent.setAgentType(isEntity ? "company" : "individual");
            agent.setMembershipType(Agent.MembershipType.agent);
            agent.setUserId(resolvedUserId);
            agent.setFalLicenseNumber(falLicense);
            agent.setCreatedAt(LocalDateTime.now());
            agentRepository.save(agent);
        }

        Estate estate = new Estate();
        estate.setId(estateRepository.findMaxId() + 1);
        estate.setStatus(Estate.Status.active);
        estate.setZoneId(zoneId);
        estate.setUserId(resolvedUserId);
        estate.setImages("[]");
        estate.setCreatedAt(LocalDateTime.now());
        estate.setUpdatedAt(LocalDateTime.now());

        estate.setLicenseNumber(form.get("licenseNumber"));
        estate.setDeedNumber(form.get("deedNumber"));
        estate.setAdvertiserName(advertiserName);
        estate.setPhoneNumber(phone);
        estate.setPropertyType(form.get("propertyType"));
        estate.setAdvertisementType(form.get("advertisementType"));
        estate.setPostalCode(parseIntOrNull(form.get("postalCode")));
        estate.setCity(form.get("city"));
        estate.setDistricts(form.get("districts"));
        estate.setLatitude(form.get("latitude"));
        estate.setLongitude(form.get("longitude"));
        estate.setCreationDate(form.get("creationDate"));
        estate.setEndDate(form.get("endDate"));
        estate.setTotalPrice(form.get("landTotalPrice"));
        estate.setPropertyFace(form.get("propertyFace"));
        estate.setAdLicenseNumber(form.get("adLicenseNumber"));
        estate.setLandNumber(form.get("landNumber"));
        estate.setTitleDeedTypeName(form.get("titleDeedTypeName"));
        estate.setAdLicenseUrl(form.get("adLicenseUrl"));
        estate.setNumberOfRooms(form.get("numberOfRooms"));
        estate.setLocationDescriptionOnMOJDeed(form.get("locationDescriptionOnMOJDeed"));
        estate.setGuaranteesAndTheirDuration(form.get("guaranteesAndTheirDuration"));
        estate.setObligationsOnTheProperty(form.get("obligationsOnTheProperty"));
        estate.setBrokerageAndMarketingLicenseNumber(falLicense);
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

    /** Combines the three border fields (name/description/length) for one side into one readable line. */
    private String borderText(JsonNode ad, String side) {
        JsonNode borders = ad.path("borders");
        String name = text(borders, side + "LimitName");
        String desc = text(borders, side + "LimitDescription");
        String length = text(borders, side + "LimitLengthChar");
        StringBuilder sb = new StringBuilder();
        if (name != null && !name.isBlank()) sb.append(name).append(" ");
        if (desc != null && !desc.isBlank()) sb.append(desc).append(" - ");
        if (length != null && !length.isBlank()) sb.append(length);
        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
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