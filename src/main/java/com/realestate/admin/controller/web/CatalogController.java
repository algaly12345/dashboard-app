package com.realestate.admin.controller.web;

import com.realestate.admin.entity.Category;
import com.realestate.admin.entity.Zone;
import com.realestate.admin.repository.CategoryRepository;
import com.realestate.admin.repository.EstateRepository;
import com.realestate.admin.repository.OfferZoneRepository;
import com.realestate.admin.repository.ZoneRepository;
import com.realestate.admin.service.R2StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
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
    private final R2StorageService r2StorageService;

    // ---------------------------------------------------------------- Categories

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryRepository.findAllByOrderByPositionAsc());
        model.addAttribute("activePage", "categories");
        return "categories";
    }

    @GetMapping("/categories/{id}/edit")
    public String editCategoryForm(@PathVariable Long id, Model model) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        model.addAttribute("category", category);
        model.addAttribute("activePage", "categories");
        return "category-edit";
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(@PathVariable Long id,
                                  @RequestParam String name,
                                  @RequestParam String nameAr,
                                  @RequestParam(required = false) String type,
                                  @RequestParam(required = false) String statusHome,
                                  RedirectAttributes redirectAttributes) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        category.setName(name);
        category.setNameAr(nameAr);
        category.setType(type);
        if (statusHome != null && !statusHome.isBlank()) {
            category.setStatusHome(Category.Status.valueOf(statusHome));
        }
        categoryRepository.save(category);
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/categories/" + id + "/edit";
    }

    @PostMapping("/categories/{id}/upload-image")
    public String uploadCategoryImage(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                       RedirectAttributes redirectAttributes) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));

        R2StorageService.UploadResult result = r2StorageService.upload(file, "categories");
        if (result.success()) {
            category.setImage(result.filename());
            categoryRepository.save(category);
            redirectAttributes.addFlashAttribute("uploadResult", true);
        } else {
            redirectAttributes.addFlashAttribute("uploadResult", false);
            redirectAttributes.addFlashAttribute("uploadError", result.error());
        }
        return "redirect:/categories/" + id + "/edit";
    }

    // ---------------------------------------------------------------- Zones

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

    @GetMapping("/zones/{id}/edit")
    public String editZoneForm(@PathVariable Long id, Model model) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + id));
        model.addAttribute("zone", zone);
        model.addAttribute("activePage", "zones");
        return "zone-edit";
    }

    @PostMapping("/zones/{id}")
    public String updateZone(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam String nameAr,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String latitude,
                              @RequestParam(required = false) String longitude,
                              RedirectAttributes redirectAttributes) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + id));
        zone.setName(name);
        zone.setNameAr(nameAr);
        if (status != null && !status.isBlank()) {
            zone.setStatus(Zone.Status.valueOf(status));
        }
        zone.setLatitude(latitude);
        zone.setLongitude(longitude);
        zoneRepository.save(zone);
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/zones/" + id + "/edit";
    }

    @PostMapping("/zones/{id}/upload-image")
    public String uploadZoneImage(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + id));

        R2StorageService.UploadResult result = r2StorageService.upload(file, "zone");
        if (result.success()) {
            zone.setImage(result.filename());
            zoneRepository.save(zone);
            redirectAttributes.addFlashAttribute("uploadResult", true);
        } else {
            redirectAttributes.addFlashAttribute("uploadResult", false);
            redirectAttributes.addFlashAttribute("uploadError", result.error());
        }
        return "redirect:/zones/" + id + "/edit";
    }
}
