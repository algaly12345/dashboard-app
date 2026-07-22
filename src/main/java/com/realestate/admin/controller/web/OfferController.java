package com.realestate.admin.controller.web;

import com.realestate.admin.entity.Category;
import com.realestate.admin.entity.Offer;
import com.realestate.admin.entity.ServiceType;
import com.realestate.admin.entity.Zone;
import com.realestate.admin.repository.AppUserRepository;
import com.realestate.admin.repository.CategoryOfferRepository;
import com.realestate.admin.repository.CategoryRepository;
import com.realestate.admin.repository.OfferRepository;
import com.realestate.admin.repository.OfferZoneRepository;
import com.realestate.admin.repository.ServiceTypeRepository;
import com.realestate.admin.repository.ZoneRepository;
import com.realestate.admin.service.R2StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
public class OfferController {

    private final OfferRepository offerRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final ZoneRepository zoneRepository;
    private final CategoryRepository categoryRepository;
    private final OfferZoneRepository offerZoneRepository;
    private final CategoryOfferRepository categoryOfferRepository;
    private final AppUserRepository appUserRepository;
    private final R2StorageService r2StorageService;

    @GetMapping("/offers")
    public String list(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String offerType,
                        @RequestParam(required = false) String serviceTypeId,
                        @RequestParam(required = false) String zoneId,
                        @RequestParam(required = false) String categoryId,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {

        Offer.OfferType offerTypeEnum = (offerType != null && !offerType.isBlank())
                ? Offer.OfferType.valueOf(offerType) : null;
        Long serviceTypeIdLong = parseLongOrNull(serviceTypeId);
        Long zoneIdLong = parseLongOrNull(zoneId);
        Long categoryIdLong = parseLongOrNull(categoryId);

        Page<Offer> result = offerRepository.search(
                blankToNull(q), blankToNull(status), offerTypeEnum,
                serviceTypeIdLong, zoneIdLong, categoryIdLong,
                PageRequest.of(page, 12, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<ServiceType> serviceTypes = serviceTypeRepository.findAllByOrderByNameAsc();
        Map<Long, String> serviceTypeNames = new HashMap<>();
        for (ServiceType st : serviceTypes) serviceTypeNames.put(st.getId(), st.getName());

        List<Zone> zones = zoneRepository.findAll();
        List<Category> categories = categoryRepository.findAllByOrderByPositionAsc();

        model.addAttribute("offers", result);
        model.addAttribute("serviceTypes", serviceTypes);
        model.addAttribute("serviceTypeNames", serviceTypeNames);
        model.addAttribute("zones", zones);
        model.addAttribute("categories", categories);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("offerType", offerType);
        model.addAttribute("serviceTypeId", serviceTypeIdLong);
        model.addAttribute("zoneId", zoneIdLong);
        model.addAttribute("categoryId", categoryIdLong);
        model.addAttribute("activePage", "offers");

        return "offers";
    }

    @GetMapping("/offers/{id}")
    public String details(@PathVariable Long id, Model model) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));

        String serviceTypeName = serviceTypeRepository.findById(offer.getServiceTypeId())
                .map(ServiceType::getName).orElse(null);

        List<Long> zoneIds = offerZoneRepository.findZoneIdsByOfferId(id);
        List<Zone> zones = zoneRepository.findAllById(zoneIds);

        List<Long> categoryIds = categoryOfferRepository.findCategoryIdsByOfferId(id);
        List<Category> categories = categoryRepository.findAllById(categoryIds);

        String providerName = offer.getPhoneProvider() != null
                ? appUserRepository.findFirstByPhoneOrderByIdAsc(offer.getPhoneProvider())
                        .map(u -> u.getName()).orElse(null)
                : null;

        model.addAttribute("offer", offer);
        model.addAttribute("serviceTypeName", serviceTypeName);
        model.addAttribute("zones", zones);
        model.addAttribute("categories", categories);
        model.addAttribute("providerName", providerName);
        model.addAttribute("activePage", "offers");
        return "offer-details";
    }

    @GetMapping("/offers/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));
        model.addAttribute("offer", offer);
        model.addAttribute("serviceTypes", serviceTypeRepository.findAllByOrderByNameAsc());
        model.addAttribute("activePage", "offers");
        return "offer-edit";
    }

    @PostMapping("/offers/{id}")
    public String update(@PathVariable Long id,
                          @RequestParam String title,
                          @RequestParam(required = false) String description,
                          @RequestParam(required = false) String servicePrice,
                          @RequestParam(required = false) String discount,
                          @RequestParam String offerType,
                          @RequestParam String status,
                          @RequestParam String expiryDate,
                          @RequestParam(required = false) String phoneProvider,
                          @RequestParam(required = false) String serviceTypeId,
                          RedirectAttributes redirectAttributes) {

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));

        offer.setTitle(title);
        offer.setDescription(description);
        offer.setServicePrice(parseIntOrNull(servicePrice));
        offer.setDiscount(parseIntOrNull(discount));
        offer.setOfferType(Offer.OfferType.valueOf(offerType));
        offer.setStatus(status);
        offer.setExpiryDate(expiryDate);
        offer.setPhoneProvider(phoneProvider);
        Long serviceTypeIdLong = parseLongOrNull(serviceTypeId);
        if (serviceTypeIdLong != null) offer.setServiceTypeId(serviceTypeIdLong);
        offer.setUpdatedAt(LocalDateTime.now());

        offerRepository.save(offer);
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/offers/" + id + "/edit";
    }

    @PostMapping("/offers/{id}/upload-image")
    public String uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + id));

        R2StorageService.UploadResult result = r2StorageService.upload(file, "offers");
        if (result.success()) {
            offer.setImage(result.filename());
            offer.setUpdatedAt(LocalDateTime.now());
            offerRepository.save(offer);
            redirectAttributes.addFlashAttribute("uploadResult", true);
        } else {
            redirectAttributes.addFlashAttribute("uploadResult", false);
            redirectAttributes.addFlashAttribute("uploadError", result.error());
        }
        return "redirect:/offers/" + id + "/edit";
    }

    /** Quick-action approval ("اعتماد الخدمة") straight from the list. */
    @PostMapping("/offers/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(required = false) String redirectTo) {
        offerRepository.findById(id).ifPresent(offer -> {
            offer.setStatus("accept");
            offer.setUpdatedAt(LocalDateTime.now());
            offerRepository.save(offer);
        });
        return "redirect:" + (redirectTo != null && !redirectTo.isBlank() ? redirectTo : "/offers");
    }

    @PostMapping("/offers/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String redirectTo) {
        offerRepository.findById(id).ifPresent(offer -> {
            offer.setStatus("reject");
            offer.setUpdatedAt(LocalDateTime.now());
            offerRepository.save(offer);
        });
        return "redirect:" + (redirectTo != null && !redirectTo.isBlank() ? redirectTo : "/offers");
    }

    private Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
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

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
