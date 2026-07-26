package com.realestate.admin.controller.web;

import com.realestate.admin.dto.SelectOption;
import com.realestate.admin.entity.AppUser;
import com.realestate.admin.entity.Banner;
import com.realestate.admin.entity.ServiceProvider;
import com.realestate.admin.entity.Zone;
import com.realestate.admin.repository.AppUserRepository;
import com.realestate.admin.repository.BannerRepository;
import com.realestate.admin.repository.ServiceProviderRepository;
import com.realestate.admin.repository.ZoneRepository;
import com.realestate.admin.service.R2StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class BannerController {

    private final BannerRepository bannerRepository;
    private final ZoneRepository zoneRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final AppUserRepository appUserRepository;
    private final R2StorageService r2StorageService;

    @GetMapping("/banners")
    public String list(Model model) {
        List<Banner> banners = bannerRepository.findAllByOrderByIdDesc();

        Map<Long, String> zoneNames = new HashMap<>();
        for (Zone z : zoneRepository.findAll()) zoneNames.put(z.getId(), z.getNameAr());

        Map<Long, String> providerNames = new HashMap<>();
        for (ServiceProvider sp : serviceProviderRepository.findAll()) {
            appUserRepository.findById(sp.getUserId()).ifPresent(u -> providerNames.put(sp.getUserId(), u.getName()));
        }

        model.addAttribute("banners", banners);
        model.addAttribute("zoneNames", zoneNames);
        model.addAttribute("providerNames", providerNames);
        model.addAttribute("activePage", "banners");
        return "banners";
    }

    @GetMapping("/banners/create")
    public String createForm(Model model) {
        model.addAttribute("banner", new Banner());
        model.addAttribute("zones", zoneRepository.findAll());
        model.addAttribute("providerOptions", providerOptions());
        model.addAttribute("activePage", "banners");
        return "banner-create";
    }

    @PostMapping("/banners")
    public String create(@RequestParam Map<String, String> form, RedirectAttributes redirectAttributes) {
        Banner banner = new Banner();
        banner.setId(bannerRepository.findMaxId() + 1);
        banner.setTitle(form.get("title"));
        banner.setDescription(form.get("description"));
        banner.setStatus(form.containsKey("status"));
        banner.setZoneId(parseLongOrNull(form.get("zoneId")));
        banner.setProviderId(parseLongOrNull(form.get("providerId")));
        banner.setCreatedAt(LocalDateTime.now());
        banner.setUpdatedAt(LocalDateTime.now());
        bannerRepository.save(banner);

        redirectAttributes.addFlashAttribute("created", true);
        return "redirect:/banners/" + banner.getId() + "/edit";
    }

    @GetMapping("/banners/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found: " + id));
        model.addAttribute("banner", banner);
        model.addAttribute("zones", zoneRepository.findAll());
        model.addAttribute("providerOptions", providerOptions());
        model.addAttribute("activePage", "banners");
        return "banner-edit";
    }

    @PostMapping("/banners/{id}")
    public String update(@PathVariable Long id, @RequestParam Map<String, String> form,
                          RedirectAttributes redirectAttributes) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found: " + id));
        banner.setTitle(form.get("title"));
        banner.setDescription(form.get("description"));
        banner.setStatus(form.containsKey("status"));
        banner.setZoneId(parseLongOrNull(form.get("zoneId")));
        banner.setProviderId(parseLongOrNull(form.get("providerId")));
        banner.setUpdatedAt(LocalDateTime.now());
        bannerRepository.save(banner);

        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/banners/" + id + "/edit";
    }

    @PostMapping("/banners/{id}/upload-image")
    public String uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found: " + id));

        R2StorageService.UploadResult result = r2StorageService.upload(file, "banners");
        if (result.success()) {
            banner.setImage(result.filename());
            banner.setUpdatedAt(LocalDateTime.now());
            bannerRepository.save(banner);
            redirectAttributes.addFlashAttribute("uploadResult", true);
        } else {
            redirectAttributes.addFlashAttribute("uploadResult", false);
            redirectAttributes.addFlashAttribute("uploadError", result.error());
        }
        return "redirect:/banners/" + id + "/edit";
    }

    @PostMapping("/banners/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bannerRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("deleted", true);
        return "redirect:/banners";
    }

    private List<SelectOption> providerOptions() {
        return serviceProviderRepository.findAll().stream()
                .map(sp -> {
                    AppUser u = appUserRepository.findById(sp.getUserId()).orElse(null);
                    String label = (u != null ? u.getName() : "#" + sp.getUserId());
                    return new SelectOption(String.valueOf(sp.getUserId()), label);
                })
                .sorted(Comparator.comparing(SelectOption::label))
                .toList();
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
