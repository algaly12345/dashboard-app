package com.realestate.admin.controller.web;

import com.realestate.admin.dto.LabelCount;
import com.realestate.admin.dto.TopProvider;
import com.realestate.admin.entity.AppUser;
import com.realestate.admin.entity.Estate;
import com.realestate.admin.entity.ServiceType;
import com.realestate.admin.entity.Zone;
import com.realestate.admin.repository.AppUserRepository;
import com.realestate.admin.repository.CategoryRepository;
import com.realestate.admin.repository.EstateRepository;
import com.realestate.admin.repository.OfferRepository;
import com.realestate.admin.repository.OfferZoneRepository;
import com.realestate.admin.repository.ServiceTypeRepository;
import com.realestate.admin.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final EstateRepository estateRepository;
    private final AppUserRepository appUserRepository;
    private final CategoryRepository categoryRepository;
    private final ZoneRepository zoneRepository;
    private final OfferZoneRepository offerZoneRepository;
    private final OfferRepository offerRepository;
    private final ServiceTypeRepository serviceTypeRepository;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "week") String period, Model model) {
        long totalEstates = estateRepository.count();
        long activeEstates = estateRepository.countByStatus(Estate.Status.active);
        long forSale = estateRepository.countByAdvertisementType("بيع");
        long forRent = estateRepository.countByAdvertisementType("إيجار");

        // Anchor "now" to the most recent listing activity instead of the
        // real server clock - see periodStart() javadoc-style comment below.
        LocalDateTime referenceNow = estateRepository.findMaxCreatedAt();
        if (referenceNow == null) referenceNow = LocalDateTime.now();

        LocalDateTime since = periodStart(period, referenceNow);
        long newInPeriod = estateRepository.countByCreatedAtGreaterThanEqual(since);

        long totalUsers = appUserRepository.count();
        long totalCategories = categoryRepository.count();
        long totalZones = zoneRepository.count();
        long totalOffers = offerRepository.count();

        List<LabelCount> byCategory = estateRepository.countGroupedByCategory().stream()
                .map(r -> new LabelCount((String) r[0], (Long) r[1]))
                .limit(8)
                .toList();

        List<LabelCount> byCity = estateRepository.countGroupedByCity().stream()
                .map(r -> new LabelCount((String) r[0], (Long) r[1]))
                .limit(8)
                .toList();

        Map<Long, String> zoneNames = new HashMap<>();
        for (Zone z : zoneRepository.findAll()) {
            zoneNames.put(z.getId(), z.getNameAr());
        }
        Map<Long, Long> estatesByZone = new HashMap<>();
        for (Object[] row : estateRepository.countGroupedByZone()) {
            estatesByZone.put((Long) row[0], (Long) row[1]);
        }
        Map<Long, Long> servicesByZone = new HashMap<>();
        for (Object[] row : offerZoneRepository.countDistinctOffersByZone()) {
            servicesByZone.put((Long) row[0], (Long) row[1]);
        }
        List<LabelCount> byZone = estatesByZone.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(8)
                .map(e -> new LabelCount(
                        zoneNames.getOrDefault(e.getKey(), "#" + e.getKey()),
                        e.getValue()))
                .toList();

        // ---- Services / providers panel ----
        Map<String, Long> statusCounts = new HashMap<>();
        for (Object[] row : offerRepository.countGroupedByStatus()) {
            statusCounts.put((String) row[0], (Long) row[1]);
        }
        long pendingOffers = statusCounts.getOrDefault("pending", 0L);
        long acceptedOffers = statusCounts.getOrDefault("accept", 0L);
        long rejectedOffers = statusCounts.getOrDefault("reject", 0L);

        Map<Long, String> serviceTypeNames = new HashMap<>();
        for (ServiceType st : serviceTypeRepository.findAll()) {
            serviceTypeNames.put(st.getId(), st.getName());
        }
        List<LabelCount> byServiceType = offerRepository.countGroupedByServiceType().stream()
                .map(r -> new LabelCount(
                        serviceTypeNames.getOrDefault((Long) r[0], "#" + r[0]),
                        (Long) r[1]))
                .limit(6)
                .toList();

        List<TopProvider> topProviders = offerRepository.countGroupedByPhoneProvider().stream()
                .sorted((a, b) -> Long.compare((Long) b[1], (Long) a[1]))
                .limit(5)
                .map(r -> {
                    String phone = (String) r[0];
                    long count = (Long) r[1];
                    String name = appUserRepository.findFirstByPhoneOrderByIdAsc(phone).map(AppUser::getName).orElse(null);
                    return new TopProvider(phone, name, count);
                })
                .toList();

        List<Estate> recent = estateRepository.findTop6ByOrderByCreatedAtDesc();

        model.addAttribute("totalEstates", totalEstates);
        model.addAttribute("activeEstates", activeEstates);
        model.addAttribute("forSale", forSale);
        model.addAttribute("forRent", forRent);
        model.addAttribute("newInPeriod", newInPeriod);
        model.addAttribute("period", period);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalCategories", totalCategories);
        model.addAttribute("totalZones", totalZones);
        model.addAttribute("totalOffers", totalOffers);
        model.addAttribute("byCategory", byCategory);
        model.addAttribute("byCity", byCity);
        model.addAttribute("byZone", byZone);
        model.addAttribute("estatesByZone", estatesByZone);
        model.addAttribute("servicesByZone", servicesByZone);
        model.addAttribute("pendingOffers", pendingOffers);
        model.addAttribute("acceptedOffers", acceptedOffers);
        model.addAttribute("rejectedOffers", rejectedOffers);
        model.addAttribute("byServiceType", byServiceType);
        model.addAttribute("topProviders", topProviders);
        model.addAttribute("recentEstates", recent);
        model.addAttribute("activePage", "dashboard");

        return "dashboard";
    }

    private LocalDateTime periodStart(String period, LocalDateTime referenceNow) {
        LocalDate refDay = referenceNow.toLocalDate();
        return switch (period) {
            case "day" -> refDay.atStartOfDay();
            case "month" -> refDay.withDayOfMonth(1).atStartOfDay();
            case "year" -> refDay.withDayOfYear(1).atStartOfDay();
            default -> refDay.minusDays(6).atStartOfDay(); // week (rolling 7 days, "day" included)
        };
    }
}
