package com.realestate.admin.controller.web;

import com.realestate.admin.dto.EstateMapPin;
import com.realestate.admin.dto.LabelCount;
import com.realestate.admin.dto.TopProvider;
import com.realestate.admin.entity.AppUser;
import com.realestate.admin.entity.Estate;
import com.realestate.admin.entity.Offer;
import com.realestate.admin.entity.ServiceType;
import com.realestate.admin.entity.Zone;
import com.realestate.admin.repository.AppUserRepository;
import com.realestate.admin.repository.CategoryRepository;
import com.realestate.admin.repository.EstateRepository;
import com.realestate.admin.repository.OfferRepository;
import com.realestate.admin.repository.ServiceTypeRepository;
import com.realestate.admin.repository.ZoneRepository;
import com.realestate.admin.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final EstateRepository estateRepository;
    private final AppUserRepository appUserRepository;
    private final CategoryRepository categoryRepository;
    private final ZoneRepository zoneRepository;
    private final OfferRepository offerRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final SettingsService settingsService;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "week") String period,
                             @RequestParam(required = false) String zoneId,
                             Model model) {

        Long zoneIdLong = parseLongOrNull(zoneId);

        // Fetch the (optionally zone-filtered) slice once, compute every
        // stat below from it in memory - same approach as the Reports
        // pages, so a zone filter here doesn't mean maintaining a second,
        // separately-filtered copy of every query.
        List<Estate> estates = estateRepository.findForReport(zoneIdLong, null, null);
        List<Offer> offers = offerRepository.findForReport(zoneIdLong, null, null);

        long totalEstates = estates.size();
        long activeEstates = estates.stream().filter(e -> e.getStatus() == Estate.Status.active).count();
        long forSale = estates.stream().filter(e -> "بيع".equals(e.getAdvertisementType())).count();
        long forRent = estates.stream().filter(e -> "إيجار".equals(e.getAdvertisementType())).count();
        long expiredLicenses = estates.stream().filter(Estate::isLicenseExpired).count();

        // Anchor "now" to the most recent listing activity instead of the
        // real server clock (historical/staging data would otherwise make
        // every period bucket read 0 - see prior fix for the full story).
        LocalDateTime referenceNow = estateRepository.findMaxCreatedAt();
        if (referenceNow == null) referenceNow = LocalDateTime.now();
        LocalDateTime since = periodStart(period, referenceNow);
        long newInPeriod = estates.stream()
                .filter(e -> e.getCreatedAt() != null && !e.getCreatedAt().isBefore(since))
                .count();

        long totalUsers = zoneIdLong != null ? appUserRepository.countByZoneId(zoneIdLong) : appUserRepository.count();
        long totalCategories = categoryRepository.count();
        long totalZones = zoneRepository.count();
        long totalOffers = offers.size();

        List<LabelCount> byCategory = estates.stream()
                .filter(e -> e.getCategoryName() != null)
                .collect(Collectors.groupingBy(Estate::getCategoryName, Collectors.counting()))
                .entrySet().stream()
                .map(en -> new LabelCount(en.getKey(), en.getValue()))
                .sorted(Comparator.comparingLong(LabelCount::count).reversed())
                .limit(8)
                .toList();

        List<LabelCount> byCity = estates.stream()
                .filter(e -> e.getCity() != null)
                .collect(Collectors.groupingBy(Estate::getCity, Collectors.counting()))
                .entrySet().stream()
                .map(en -> new LabelCount(en.getKey(), en.getValue()))
                .sorted(Comparator.comparingLong(LabelCount::count).reversed())
                .limit(8)
                .toList();

        Map<Long, String> zoneNames = new HashMap<>();
        for (Zone z : zoneRepository.findAll()) zoneNames.put(z.getId(), z.getNameAr());
        List<LabelCount> byZone = estates.stream()
                .filter(e -> e.getZoneId() != null)
                .collect(Collectors.groupingBy(Estate::getZoneId, Collectors.counting()))
                .entrySet().stream()
                .map(en -> new LabelCount(zoneNames.getOrDefault(en.getKey(), "#" + en.getKey()), en.getValue()))
                .sorted(Comparator.comparingLong(LabelCount::count).reversed())
                .limit(8)
                .toList();

        // ---- Services / providers panel ----
        long pendingOffers = offers.stream().filter(o -> "pending".equals(o.getStatus())).count();
        long acceptedOffers = offers.stream().filter(o -> "accept".equals(o.getStatus())).count();
        long rejectedOffers = offers.stream().filter(o -> "reject".equals(o.getStatus())).count();

        Map<Long, String> serviceTypeNames = new HashMap<>();
        for (ServiceType st : serviceTypeRepository.findAll()) serviceTypeNames.put(st.getId(), st.getName());
        List<LabelCount> byServiceType = offers.stream()
                .collect(Collectors.groupingBy(Offer::getServiceTypeId, Collectors.counting()))
                .entrySet().stream()
                .map(en -> new LabelCount(serviceTypeNames.getOrDefault(en.getKey(), "#" + en.getKey()), en.getValue()))
                .sorted(Comparator.comparingLong(LabelCount::count).reversed())
                .limit(6)
                .toList();

        List<TopProvider> topProviders = offers.stream()
                .filter(o -> o.getPhoneProvider() != null)
                .collect(Collectors.groupingBy(Offer::getPhoneProvider, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(en -> {
                    String phone = en.getKey();
                    String name = appUserRepository.findFirstByPhoneOrderByIdAsc(phone).map(AppUser::getName).orElse(null);
                    return new TopProvider(phone, name, en.getValue());
                })
                .toList();

        List<Estate> recent = estates.stream()
                .sorted(Comparator.comparing(Estate::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .toList();

        List<Estate> expiredList = estates.stream()
                .filter(Estate::isLicenseExpired)
                .sorted(Comparator.comparing(Estate::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();

        List<EstateMapPin> mapPins = estates.stream()
                .filter(e -> e.getLatitude() != null && !e.getLatitude().isBlank()
                        && e.getLongitude() != null && !e.getLongitude().isBlank())
                .sorted(Comparator.comparing(Estate::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(40)
                .map(EstateMapPin::from)
                .toList();
        String mapApiKey = settingsService.get("map_api_key", "AIzaSyAwM15LYUky7qqVuXdBQc9zavA39y487jQ");

        model.addAttribute("totalEstates", totalEstates);
        model.addAttribute("activeEstates", activeEstates);
        model.addAttribute("forSale", forSale);
        model.addAttribute("forRent", forRent);
        model.addAttribute("expiredLicenses", expiredLicenses);
        model.addAttribute("expiredList", expiredList);
        model.addAttribute("newInPeriod", newInPeriod);
        model.addAttribute("period", period);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalCategories", totalCategories);
        model.addAttribute("totalZones", totalZones);
        model.addAttribute("totalOffers", totalOffers);
        model.addAttribute("byCategory", byCategory);
        model.addAttribute("byCity", byCity);
        model.addAttribute("byZone", byZone);
        model.addAttribute("pendingOffers", pendingOffers);
        model.addAttribute("acceptedOffers", acceptedOffers);
        model.addAttribute("rejectedOffers", rejectedOffers);
        model.addAttribute("byServiceType", byServiceType);
        model.addAttribute("topProviders", topProviders);
        model.addAttribute("recentEstates", recent);
        model.addAttribute("mapPins", mapPins);
        model.addAttribute("mapApiKey", mapApiKey);
        model.addAttribute("zones", zoneRepository.findAll());
        model.addAttribute("zoneId", zoneIdLong);
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

    private Long parseLongOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
