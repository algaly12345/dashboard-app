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
                          // agent profile fields (present only if the user has an agent row)
                          @RequestParam(required = false) String agentType,
                          @RequestParam(required = false) String agentMembershipType,
                          @RequestParam(required = false) String falLicenseNumber,
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
        appUserRepository.save(user);

        agentRepository.findByUserId(id).ifPresent(agent -> {
            agent.setAgentType(agentType);
            if (agentMembershipType != null && !agentMembershipType.isBlank()) {
                agent.setMembershipType(Agent.MembershipType.valueOf(agentMembershipType));
            }
            agent.setFalLicenseNumber(falLicenseNumber);
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

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
