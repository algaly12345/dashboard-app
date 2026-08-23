package com.realestate.admin.controller.web;

import com.realestate.admin.entity.Agent;
import com.realestate.admin.entity.AppUser;
import com.realestate.admin.entity.ServiceProvider;
import com.realestate.admin.repository.AgentRepository;
import com.realestate.admin.repository.AppUserRepository;
import com.realestate.admin.repository.EstateRepository;
import com.realestate.admin.repository.OfferRepository;
import com.realestate.admin.repository.ServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final AppUserRepository appUserRepository;
    private final AgentRepository agentRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final EstateRepository estateRepository;
    private final OfferRepository offerRepository;

    @GetMapping("/users")
    public String list(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String userType,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {

        Page<AppUser> result = appUserRepository.search(
                blankToNull(q), blankToNull(userType), PageRequest.of(page, 15));

        Map<Long, Long> estateCounts = new HashMap<>();
        for (Object[] row : estateRepository.countGroupedByUser()) {
            estateCounts.put((Long) row[0], (Long) row[1]);
        }

        // Offers have no user_id - the creator is identified by phone_provider,
        // matched against users.phone. Build a phone -> count map, then key
        // it by user id for the template.
        Map<String, Long> offersByPhone = new HashMap<>();
        for (Object[] row : offerRepository.countGroupedByPhoneProvider()) {
            offersByPhone.put((String) row[0], (Long) row[1]);
        }
        Map<Long, Long> offerCounts = new HashMap<>();
        for (AppUser u : result.getContent()) {
            Long count = offersByPhone.get(u.getPhone());
            if (count != null) offerCounts.put(u.getId(), count);
        }

        model.addAttribute("users", result);
        model.addAttribute("estateCounts", estateCounts);
        model.addAttribute("offerCounts", offerCounts);
        model.addAttribute("q", q);
        model.addAttribute("userType", userType);
        model.addAttribute("activePage", "users");

        return "users";
    }

    /**
     * A platform account is one row in `users`, plus at most one role
     * profile: `agents` (marketer / property seeker) or `service_providers`
     * (service provider). We show + edit both in one screen, plus how many
     * listings (agent) or service offers (provider) that account has added.
     * Offers have no user_id column - the provider is matched by phone
     * (offers.phone_provider == users.phone).
     */
    @GetMapping("/users/{id}")
    public String details(@PathVariable Long id, Model model) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        Agent agent = agentRepository.findByUserId(id).orElse(null);
        ServiceProvider provider = serviceProviderRepository.findByUserId(id).orElse(null);

        long estateCount = estateRepository.countByUserId(id);
        long offerCount = user.getPhone() != null ? offerRepository.countByPhoneProvider(user.getPhone()) : 0;

        model.addAttribute("user", user);
        model.addAttribute("agent", agent);
        model.addAttribute("provider", provider);
        model.addAttribute("estateCount", estateCount);
        model.addAttribute("offerCount", offerCount);
        model.addAttribute("activePage", "users");
        return "user-details";
    }

    @PostMapping("/users/{id}")
    public String update(@PathVariable Long id,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) String phone,
                          @RequestParam(required = false) String email,
                          @RequestParam(required = false) String isActive,
                          @RequestParam(required = false) String unifiedNumber,
                          @RequestParam(required = false) Integer advertiserNo,
                          @RequestParam(required = false) java.math.BigDecimal walletBalance,
                          @RequestParam(required = false) java.math.BigDecimal loyaltyPoint,
                          @RequestParam(required = false) String zoneId,
                          @RequestParam(required = false) String cityId,
                          @RequestParam(required = false) String userFalLicenseNumber,
                          @RequestParam(required = false) String youtube,
                          @RequestParam(required = false) String snapchat,
                          @RequestParam(required = false) String instagram,
                          @RequestParam(required = false) String website,
                          @RequestParam(required = false) String tiktok,
                          @RequestParam(required = false) String twitter,
                          @RequestParam(required = false) String isPhoneVerified,
                          @RequestParam(required = false) String isEmailVerified,
                          @RequestParam(required = false) String isTempBlocked,
                          @RequestParam(required = false) String accountVerification,
                          // agent profile fields (present only if the user has an agent row)
                          @RequestParam(required = false) String agentType,
                          @RequestParam(required = false) String agentMembershipType,
                          @RequestParam(required = false) String falLicenseNumber,
                          @RequestParam(required = false) String identityType,
                          @RequestParam(required = false) String commercialRegisterionNo,
                          // service-provider profile fields (present only if the user has one)
                          @RequestParam(required = false) String job,
                          @RequestParam(required = false) String providerAddress,
                          @RequestParam(required = false) String identityNumber,
                          RedirectAttributes redirectAttributes) {

        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setName(name);
        user.setPhone(phone);
        user.setEmail(email);
        if (isActive != null && !isActive.isBlank()) {
            user.setIsActive(AppUser.Status.valueOf(isActive));
        }
        user.setUnifiedNumber(unifiedNumber);
        user.setAdvertiserNo(advertiserNo);
        user.setWalletBalance(walletBalance);
        user.setLoyaltyPoint(loyaltyPoint);
        user.setZoneId(parseLongOrNull(zoneId));
        user.setCityId(parseLongOrNull(cityId));
        user.setFalLicenseNumber(userFalLicenseNumber);
        user.setYoutube(youtube);
        user.setSnapchat(snapchat);
        user.setInstagram(instagram);
        user.setWebsite(website);
        user.setTiktok(tiktok);
        user.setTwitter(twitter);
        user.setIsPhoneVerified("true".equals(isPhoneVerified) ? 1 : 0);
        user.setIsEmailVerified("true".equals(isEmailVerified) ? 1 : 0);
        user.setIsTempBlocked("true".equals(isTempBlocked) ? 1 : 0);
        user.setAccountVerification("true".equals(accountVerification));
        appUserRepository.save(user);

        agentRepository.findByUserId(id).ifPresent(agent -> {
            agent.setAgentType(agentType);
            if (agentMembershipType != null && !agentMembershipType.isBlank()) {
                agent.setMembershipType(Agent.MembershipType.valueOf(agentMembershipType));
            }
            agent.setFalLicenseNumber(falLicenseNumber);
            agent.setIdentityType(identityType);
            agent.setCommercialRegisterionNo(commercialRegisterionNo);
            agentRepository.save(agent);
        });

        serviceProviderRepository.findByUserId(id).ifPresent(provider -> {
            provider.setJob(job);
            provider.setAddress(providerAddress);
            provider.setIdentityNumber(identityNumber);
            serviceProviderRepository.save(provider);
        });

        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/users/" + id;
    }

    @GetMapping("/users/new")
    public String newForm(Model model) {
        model.addAttribute("activePage", "users");
        return "user-new";
    }

    @PostMapping("/users")
    public String create(@RequestParam String name,
                          @RequestParam String phone,
                          @RequestParam(required = false) String email,
                          @RequestParam String userType,
                          @RequestParam(required = false) String zoneId,
                          RedirectAttributes redirectAttributes) {

        AppUser user = new AppUser();
        user.setId(appUserRepository.findMaxId() + 1);
        user.setName(name);
        user.setPhone(phone);
        user.setEmail(email);
        user.setUserType(userType);
        user.setPassword(java.util.UUID.randomUUID().toString());
        user.setIsActive(AppUser.Status.active);
        user.setAccountVerification(false);
        user.setIsPhoneVerified(0);
        user.setIsEmailVerified(0);
        user.setIsTempBlocked(0);
        user.setLoyaltyPoint(java.math.BigDecimal.ZERO);
        user.setZoneId(parseLongOrNull(zoneId));
        user.setWalletBalance(java.math.BigDecimal.ZERO);
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        appUserRepository.save(user);

        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/users/" + user.getId();
    }

    private Long parseLongOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
