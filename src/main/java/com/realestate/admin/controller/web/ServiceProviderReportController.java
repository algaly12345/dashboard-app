package com.realestate.admin.controller.web;

import com.realestate.admin.dto.LabelCount;
import com.realestate.admin.dto.MonthStat;
import com.realestate.admin.dto.SelectOption;
import com.realestate.admin.dto.TopProvider;
import com.realestate.admin.entity.AppUser;
import com.realestate.admin.entity.Offer;
import com.realestate.admin.entity.OfferZone;
import com.realestate.admin.entity.ServiceType;
import com.realestate.admin.entity.Zone;
import com.realestate.admin.repository.AppUserRepository;
import com.realestate.admin.repository.OfferRepository;
import com.realestate.admin.repository.OfferZoneRepository;
import com.realestate.admin.repository.ServiceTypeRepository;
import com.realestate.admin.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ServiceProviderReportController {

    private final OfferRepository offerRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final ZoneRepository zoneRepository;
    private final OfferZoneRepository offerZoneRepository;
    private final AppUserRepository appUserRepository;

    @GetMapping("/provider-reports")
    public String report(@RequestParam(required = false) String zoneId,
                          @RequestParam(required = false) String serviceTypeId,
                          @RequestParam(required = false) String phoneProvider,
                          Model model) {

        Long zoneIdLong = parseLongOrNull(zoneId);
        Long serviceTypeIdLong = parseLongOrNull(serviceTypeId);
        String phoneProviderVal = (phoneProvider == null || phoneProvider.isBlank()) ? null : phoneProvider;

        // Filtered slice once, everything below computed from it in memory.
        List<Offer> offers = offerRepository.findForReport(zoneIdLong, serviceTypeIdLong, phoneProviderVal);

        long totalOffers = offers.size();
        long pendingOffers = offers.stream().filter(o -> "pending".equals(o.getStatus())).count();
        long acceptedOffers = offers.stream().filter(o -> "accept".equals(o.getStatus())).count();
        long rejectedOffers = offers.stream().filter(o -> "reject".equals(o.getStatus())).count();

        long totalProviders = offers.stream().map(Offer::getPhoneProvider)
                .filter(java.util.Objects::nonNull).distinct().count();

        List<Integer> discounts = offers.stream()
                .filter(o -> o.getOfferType() == Offer.OfferType.discount && o.getDiscount() != null)
                .map(Offer::getDiscount).toList();
        BigDecimal avgDiscount = discounts.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(discounts.stream().mapToInt(Integer::intValue).average().orElse(0))
                        .setScale(0, RoundingMode.HALF_UP);

        List<Integer> prices = offers.stream()
                .filter(o -> o.getOfferType() == Offer.OfferType.price && o.getServicePrice() != null)
                .map(Offer::getServicePrice).toList();
        BigDecimal avgServicePrice = prices.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(prices.stream().mapToInt(Integer::intValue).average().orElse(0))
                        .setScale(0, RoundingMode.HALF_UP);

        // ---- Top providers ----
        List<TopProvider> topProviders = offers.stream()
                .filter(o -> o.getPhoneProvider() != null)
                .collect(Collectors.groupingBy(Offer::getPhoneProvider, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(en -> {
                    String phone = en.getKey();
                    String name = appUserRepository.findFirstByPhoneOrderByIdAsc(phone)
                            .map(AppUser::getName).orElse(null);
                    return new TopProvider(phone, name, en.getValue());
                })
                .toList();

        // ---- By service type ----
        Map<Long, String> serviceTypeNames = new HashMap<>();
        for (ServiceType st : serviceTypeRepository.findAll()) serviceTypeNames.put(st.getId(), st.getName());
        List<LabelCount> byServiceType = offers.stream()
                .collect(Collectors.groupingBy(Offer::getServiceTypeId, Collectors.counting()))
                .entrySet().stream()
                .map(en -> new LabelCount(serviceTypeNames.getOrDefault(en.getKey(), "#" + en.getKey()), en.getValue()))
                .sorted(Comparator.comparingLong(LabelCount::count).reversed())
                .limit(10)
                .toList();

        // ---- By zone (via offer_zone, across the filtered offer ids) ----
        Map<Long, String> zoneNames = new HashMap<>();
        for (Zone z : zoneRepository.findAll()) zoneNames.put(z.getId(), z.getNameAr());
        List<Long> offerIds = offers.stream().map(Offer::getId).toList();
        List<OfferZone> offerZones = offerIds.isEmpty() ? List.of() : offerZoneRepository.findByOfferIdIn(offerIds);
        List<LabelCount> byZone = offerZones.stream()
                .collect(Collectors.groupingBy(OfferZone::getZoneId, Collectors.counting()))
                .entrySet().stream()
                .map(en -> new LabelCount(zoneNames.getOrDefault(en.getKey(), "#" + en.getKey()), en.getValue()))
                .sorted(Comparator.comparingLong(LabelCount::count).reversed())
                .limit(10)
                .toList();

        // ---- Monthly trend ----
        Map<YearMonth, Long> byMonth = new TreeMap<>();
        for (Offer o : offers) {
            if (o.getCreatedAt() == null) continue;
            byMonth.merge(YearMonth.from(o.getCreatedAt()), 1L, Long::sum);
        }
        List<Map.Entry<YearMonth, Long>> monthEntries = new ArrayList<>(byMonth.entrySet());
        int from = Math.max(0, monthEntries.size() - 12);
        List<MonthStat> monthlyTrend = monthEntries.subList(from, monthEntries.size()).stream()
                .map(en -> new MonthStat(
                        en.getKey().getMonth().getDisplayName(TextStyle.SHORT, new Locale("ar")) + " " + en.getKey().getYear(),
                        en.getValue()))
                .toList();
        long maxMonthCount = monthlyTrend.stream().mapToLong(MonthStat::count).max().orElse(1);
        if (maxMonthCount == 0) maxMonthCount = 1;

        // ---- Filter dropdown data ----
        List<Zone> zones = zoneRepository.findAll();
        List<ServiceType> serviceTypes = serviceTypeRepository.findAllByOrderByNameAsc();
        List<SelectOption> providerOptions = offerRepository.countGroupedByPhoneProvider().stream()
                .map(r -> {
                    String phone = (String) r[0];
                    String name = appUserRepository.findFirstByPhoneOrderByIdAsc(phone)
                            .map(AppUser::getName).orElse(null);
                    String label = (name != null ? name + " (" + phone + ")" : phone);
                    return new SelectOption(phone, label);
                })
                .sorted(Comparator.comparing(SelectOption::label))
                .toList();

        model.addAttribute("totalOffers", totalOffers);
        model.addAttribute("totalProviders", totalProviders);
        model.addAttribute("pendingOffers", pendingOffers);
        model.addAttribute("acceptedOffers", acceptedOffers);
        model.addAttribute("rejectedOffers", rejectedOffers);
        model.addAttribute("avgDiscount", avgDiscount);
        model.addAttribute("avgServicePrice", avgServicePrice);
        model.addAttribute("topProviders", topProviders);
        model.addAttribute("byServiceType", byServiceType);
        model.addAttribute("byZone", byZone);
        model.addAttribute("monthlyTrend", monthlyTrend);
        model.addAttribute("maxMonthCount", maxMonthCount);
        model.addAttribute("generatedAt", LocalDateTime.now());
        model.addAttribute("activePage", "provider-reports");

        model.addAttribute("zones", zones);
        model.addAttribute("serviceTypes", serviceTypes);
        model.addAttribute("providerOptions", providerOptions);
        model.addAttribute("zoneId", zoneIdLong);
        model.addAttribute("serviceTypeId", serviceTypeIdLong);
        model.addAttribute("phoneProvider", phoneProviderVal);

        return "provider-reports";
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
