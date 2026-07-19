package com.realestate.admin.controller.web;

import com.realestate.admin.entity.Zone;
import com.realestate.admin.repository.CategoryRepository;
import com.realestate.admin.repository.EstateRepository;
import com.realestate.admin.repository.OfferZoneRepository;
import com.realestate.admin.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CatalogController {

    private final CategoryRepository categoryRepository;
    private final ZoneRepository zoneRepository;
    private final EstateRepository estateRepository;
    private final OfferZoneRepository offerZoneRepository;

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryRepository.findAllByOrderByPositionAsc());
        model.addAttribute("activePage", "categories");
        return "categories";
    }

    @GetMapping("/zones")
    public String zones(Model model) {
        List<Zone> zones = zoneRepository.findAll();

        Map<Long, Long> estateCounts = new HashMap<>();
        for (Object[] row : estateRepository.countGroupedByZone()) {
            estateCounts.put((Long) row[0], (Long) row[1]);
        }

        Map<Long, Long> serviceCounts = new HashMap<>();
        for (Object[] row : offerZoneRepository.countDistinctOffersByZone()) {
            serviceCounts.put((Long) row[0], (Long) row[1]);
        }

        model.addAttribute("zones", zones);
        model.addAttribute("estateCounts", estateCounts);
        model.addAttribute("serviceCounts", serviceCounts);
        model.addAttribute("activePage", "zones");
        return "zones";
    }
}
