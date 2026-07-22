package com.realestate.admin.controller.web;

import com.realestate.admin.dto.CityStat;
import com.realestate.admin.dto.LabelCount;
import com.realestate.admin.dto.MonthStat;
import com.realestate.admin.dto.SelectOption;
import com.realestate.admin.dto.TopAgent;
import com.realestate.admin.entity.AppUser;
import com.realestate.admin.entity.Category;
import com.realestate.admin.entity.Estate;
import com.realestate.admin.entity.Zone;
import com.realestate.admin.repository.AppUserRepository;
import com.realestate.admin.repository.CategoryRepository;
import com.realestate.admin.repository.EstateRepository;
import com.realestate.admin.repository.OfferRepository;
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
public class PropertyReportController {

    private final EstateRepository estateRepository;
    private final AppUserRepository appUserRepository;
    private final CategoryRepository categoryRepository;
    private final ZoneRepository zoneRepository;
    private final OfferRepository offerRepository;

    @GetMapping("/property-reports")
    public String report(@RequestParam(required = false) String zoneId,
                          @RequestParam(required = false) String categoryId,
                          @RequestParam(required = false) String userId,
                          Model model) {

        Long zoneIdLong = parseLongOrNull(zoneId);
        Long categoryIdLong = parseLongOrNull(categoryId);
        Integer categoryIdInt = categoryIdLong != null ? categoryIdLong.intValue() : null;
        Long userIdLong = parseLongOrNull(userId);

        // Fetch the filtered slice once, compute every stat below from it in
        // memory - simpler and always consistent with the filters than
        // maintaining a dozen separately-filtered SQL aggregate queries.
        List<Estate> estates = estateRepository.findForReport(zoneIdLong, categoryIdInt, userIdLong);

        long totalEstates = estates.size();
        long activeEstates = estates.stream().filter(e -> e.getStatus() == Estate.Status.active).count();
        long forSale = estates.stream().filter(e -> "بيع".equals(e.getAdvertisementType())).count();
        long forRent = estates.stream().filter(e -> "إيجار".equals(e.getAdvertisementType())).count();
        long expiredLicenses = estates.stream().filter(Estate::isLicenseExpired).count();
        long totalUsers = appUserRepository.count();
        long totalZones = zoneRepository.count();
        long totalCategories = categoryRepository.count();
        long totalOffers = offerRepository.count();

        List<BigDecimal> salePrices = estates.stream()
                .filter(e -> "بيع".equals(e.getAdvertisementType()))
                .map(e -> parsePrice(e.getPrice()))
                .filter(java.util.Objects::nonNull)
                .toList();
        BigDecimal portfolioValue = salePrices.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal avgSalePrice = salePrices.isEmpty() ? BigDecimal.ZERO
                : portfolioValue.divide(BigDecimal.valueOf(salePrices.size()), 0, RoundingMode.HALF_UP);

        long finalTotalEstates = totalEstates;

        // ---- City breakdown ----
        List<CityStat> cityStats = estates.stream()
                .filter(e -> e.getCity() != null)
                .collect(Collectors.groupingBy(Estate::getCity))
                .entrySet().stream()
                .map(en -> buildStat(en.getKey(), en.getValue(), finalTotalEstates))
                .sorted(Comparator.comparingLong(CityStat::count).reversed())
                .limit(10)
                .toList();

        // ---- Zone breakdown ----
        Map<Long, String> zoneNames = new HashMap<>();
        for (Zone z : zoneRepository.findAll()) zoneNames.put(z.getId(), z.getNameAr());
        List<CityStat> zoneStats = estates.stream()
                .filter(e -> e.getZoneId() != null)
                .collect(Collectors.groupingBy(Estate::getZoneId))
                .entrySet().stream()
                .map(en -> buildStat(zoneNames.getOrDefault(en.getKey(), "#" + en.getKey()), en.getValue(), finalTotalEstates))
                .sorted(Comparator.comparingLong(CityStat::count).reversed())
                .limit(10)
                .toList();

        // ---- Category breakdown ----
        List<LabelCount> byCategory = estates.stream()
                .filter(e -> e.getCategoryName() != null)
                .collect(Collectors.groupingBy(Estate::getCategoryName, Collectors.counting()))
                .entrySet().stream()
                .map(en -> new LabelCount(en.getKey(), en.getValue()))
                .sorted(Comparator.comparingLong(LabelCount::count).reversed())
                .limit(10)
                .toList();

        // ---- Top marketers (who posted the listings) ----
        List<TopAgent> topAgents = estates.stream()
                .filter(e -> e.getUserId() != null)
                .collect(Collectors.groupingBy(Estate::getUserId, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .map(en -> {
                    AppUser u = appUserRepository.findById(en.getKey()).orElse(null);
                    return new TopAgent(u != null ? u.getName() : null, u != null ? u.getPhone() : null, en.getValue());
                })
                .toList();

        // ---- Monthly trend, last 12 months present in the filtered set ----
        Map<YearMonth, Long> byMonth = new TreeMap<>();
        for (Estate e : estates) {
            if (e.getCreatedAt() == null) continue;
            YearMonth ym = YearMonth.from(e.getCreatedAt());
            byMonth.merge(ym, 1L, Long::sum);
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
        List<Category> categories = categoryRepository.findAllByOrderByPositionAsc();
        List<SelectOption> marketerOptions = estateRepository.findDistinctUserIds().stream()
                .map(id -> {
                    AppUser u = appUserRepository.findById(id).orElse(null);
                    if (u == null) return null;
                    String label = u.getName() + " (" + u.getPhone() + ")";
                    return new SelectOption(String.valueOf(u.getId()), label);
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(SelectOption::label))
                .toList();

        model.addAttribute("totalEstates", totalEstates);
        model.addAttribute("activeEstates", activeEstates);
        model.addAttribute("forSale", forSale);
        model.addAttribute("forRent", forRent);
        model.addAttribute("expiredLicenses", expiredLicenses);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalZones", totalZones);
        model.addAttribute("totalCategories", totalCategories);
        model.addAttribute("totalOffers", totalOffers);
        model.addAttribute("portfolioValue", portfolioValue);
        model.addAttribute("avgSalePrice", avgSalePrice);
        model.addAttribute("cityStats", cityStats);
        model.addAttribute("zoneStats", zoneStats);
        model.addAttribute("byCategory", byCategory);
        model.addAttribute("topAgents", topAgents);
        model.addAttribute("monthlyTrend", monthlyTrend);
        model.addAttribute("maxMonthCount", maxMonthCount);
        model.addAttribute("generatedAt", LocalDateTime.now());
        model.addAttribute("activePage", "property-reports");

        model.addAttribute("zones", zones);
        model.addAttribute("categories", categories);
        model.addAttribute("marketerOptions", marketerOptions);
        model.addAttribute("zoneId", zoneIdLong);
        model.addAttribute("categoryId", categoryIdLong);
        model.addAttribute("userId", userIdLong);
        model.addAttribute("userIdParam", userId);

        return "property-reports";
    }

    private CityStat buildStat(String label, List<Estate> group, long totalEstates) {
        long count = group.size();
        List<BigDecimal> prices = group.stream().map(e -> parsePrice(e.getPrice()))
                .filter(java.util.Objects::nonNull).toList();
        BigDecimal avg = prices.isEmpty() ? BigDecimal.ZERO
                : prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(prices.size()), 0, RoundingMode.HALF_UP);
        double pct = totalEstates > 0 ? (count * 100.0 / totalEstates) : 0;
        return new CityStat(label, count, pct, avg);
    }

    static BigDecimal parsePrice(String price) {
        if (price == null || price.isBlank()) return null;
        try {
            return new BigDecimal(price.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String monthLabel(String ym) {
        try {
            YearMonth yearMonth = YearMonth.parse(ym);
            return yearMonth.getMonth().getDisplayName(TextStyle.SHORT, new Locale("ar")) + " " + yearMonth.getYear();
        } catch (Exception e) {
            return ym;
        }
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
