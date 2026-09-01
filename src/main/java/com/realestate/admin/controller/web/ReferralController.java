package com.realestate.admin.controller.web;

import com.realestate.admin.entity.AppUser;
import com.realestate.admin.entity.Referral;
import com.realestate.admin.entity.ReferralSettings;
import com.realestate.admin.repository.AppUserRepository;
import com.realestate.admin.repository.ReferralRepository;
import com.realestate.admin.repository.ReferralSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralRepository referralRepository;
    private final ReferralSettingsRepository referralSettingsRepository;
    private final AppUserRepository appUserRepository;

    @GetMapping("/referrals")
    public String list(@RequestParam(required = false) String status,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {

        Referral.Status statusEnum = (status != null && !status.isBlank())
                ? Referral.Status.valueOf(status) : null;

        Page<Referral> result = referralRepository.search(statusEnum, PageRequest.of(page, 15));

        List<Long> userIds = result.getContent().stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getReferrerId(), r.getReferredId()))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, AppUser> users = new HashMap<>();
        for (AppUser u : appUserRepository.findAllById(userIds)) users.put(u.getId(), u);

        Map<String, Long> statusCounts = new HashMap<>();
        for (Object[] row : referralRepository.countGroupedByStatus()) {
            statusCounts.put(((Referral.Status) row[0]).name(), (Long) row[1]);
        }

        model.addAttribute("referrals", result);
        model.addAttribute("users", users);
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("status", status);
        model.addAttribute("activePage", "referrals");

        return "referrals";
    }

    @PostMapping("/referrals/{id}/status")
    public String updateStatus(@PathVariable Long id,
                                @RequestParam String status,
                                RedirectAttributes redirectAttributes) {
        referralRepository.findById(id).ifPresent(referral -> {
            referral.setStatus(Referral.Status.valueOf(status));
            referral.setUpdatedAt(LocalDateTime.now());
            referralRepository.save(referral);
        });
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/referrals";
    }

    @GetMapping("/referrals/settings")
    public String settingsForm(Model model) {
        ReferralSettings settings = referralSettingsRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    ReferralSettings s = new ReferralSettings();
                    s.setId(1L);
                    s.setRewardType(ReferralSettings.RewardType.PERCENTAGE);
                    s.setRewardValue(BigDecimal.ZERO);
                    s.setAttributionWindowDays(30);
                    s.setMinPayoutLimit(BigDecimal.ZERO);
                    s.setCommissionHoldDays(14);
                    s.setCreatedAt(LocalDateTime.now());
                    s.setUpdatedAt(LocalDateTime.now());
                    return s;
                });
        model.addAttribute("settings", settings);
        model.addAttribute("activePage", "referrals");
        return "referral-settings";
    }

    @PostMapping("/referrals/settings")
    public String saveSettings(@RequestParam String rewardType,
                                @RequestParam BigDecimal rewardValue,
                                @RequestParam Integer attributionWindowDays,
                                @RequestParam BigDecimal minPayoutLimit,
                                @RequestParam(required = false) Integer commissionHoldDays,
                                RedirectAttributes redirectAttributes) {

        ReferralSettings settings = referralSettingsRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    ReferralSettings s = new ReferralSettings();
                    s.setId(1L);
                    s.setCreatedAt(LocalDateTime.now());
                    return s;
                });

        settings.setRewardType(ReferralSettings.RewardType.valueOf(rewardType));
        settings.setRewardValue(rewardValue);
        settings.setAttributionWindowDays(attributionWindowDays);
        settings.setMinPayoutLimit(minPayoutLimit);
        settings.setCommissionHoldDays(commissionHoldDays);
        settings.setUpdatedAt(LocalDateTime.now());
        referralSettingsRepository.save(settings);

        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/referrals/settings";
    }
}
