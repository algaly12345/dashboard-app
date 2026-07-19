package com.realestate.admin.controller.web;

import com.realestate.admin.repository.CategoryRepository;
import com.realestate.admin.repository.EstateRepository;
import com.realestate.admin.repository.ZoneRepository;
import com.realestate.admin.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class EstateMapController {

    private final ZoneRepository zoneRepository;
    private final CategoryRepository categoryRepository;
    private final EstateRepository estateRepository;
    private final SettingsService settingsService;

    @GetMapping("/estates/map")
    public String map(Model model) {
        model.addAttribute("zones", zoneRepository.findAll());
        model.addAttribute("categories", categoryRepository.findAllByOrderByPositionAsc());
        model.addAttribute("cities", estateRepository.findDistinctCities());
        model.addAttribute("advertisers", estateRepository.findDistinctAdvertiserNames());
        model.addAttribute("mapApiKey", settingsService.get("map_api_key", ""));
        model.addAttribute("activePage", "estates");
        return "estates-map";
    }
}
