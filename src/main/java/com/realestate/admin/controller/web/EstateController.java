package com.realestate.admin.controller.web;

import com.realestate.admin.entity.Estate;
import com.realestate.admin.repository.EstateRepository;
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
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class EstateController {

    private final EstateRepository estateRepository;
    private final R2StorageService r2StorageService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @GetMapping("/estates")
    public String list(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String city,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String adType,
                        @RequestParam(required = false) String estateType,
                        @RequestParam(required = false) String virtualTour,
                        @RequestParam(required = false) String minPrice,
                        @RequestParam(required = false) String maxPrice,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {

        Estate.Status statusEnum = (status != null && !status.isBlank()) ? Estate.Status.valueOf(status) : null;
        Boolean virtualTourBool = (virtualTour == null || virtualTour.isBlank()) ? null : "yes".equals(virtualTour);
        Double minPriceVal = parseDoubleOrNull(minPrice);
        Double maxPriceVal = parseDoubleOrNull(maxPrice);

        Page<Estate> result = estateRepository.search(
                blankToNull(q), statusEnum, blankToNull(city), blankToNull(category), blankToNull(adType),
                blankToNull(estateType), virtualTourBool, minPriceVal, maxPriceVal,
                PageRequest.of(page, 12, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<String> cities = estateRepository.findDistinctCities();
        List<String> categories = estateRepository.findDistinctCategoryNames();
        List<String> estateTypes = estateRepository.findDistinctEstateTypes();

        model.addAttribute("estates", result);
        model.addAttribute("cities", cities);
        model.addAttribute("categories", categories);
        model.addAttribute("estateTypes", estateTypes);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("city", city);
        model.addAttribute("category", category);
        model.addAttribute("adType", adType);
        model.addAttribute("estateType", estateType);
        model.addAttribute("virtualTour", virtualTour);
        model.addAttribute("minPrice", minPriceVal);
        model.addAttribute("maxPrice", maxPriceVal);
        model.addAttribute("activePage", "estates");

        return "estates";
    }

    @GetMapping("/estates/{id}")
    public String details(@PathVariable Long id, Model model) {
        Estate estate = estateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estate not found: " + id));
        model.addAttribute("estate", estate);
        model.addAttribute("activePage", "estates");
        return "estate-details";
    }

    @GetMapping("/estates/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Estate estate = estateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estate not found: " + id));
        model.addAttribute("estate", estate);
        model.addAttribute("activePage", "estates");
        return "estate-edit";
    }

    @PostMapping("/estates/{id}")
    public String update(@PathVariable Long id,
                          @RequestParam(required = false) String title,
                          @RequestParam(required = false) String shortDescription,
                          @RequestParam(required = false) String longDescription,
                          @RequestParam(required = false) String advertisementType,
                          @RequestParam(required = false) String estateType,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String price,
                          @RequestParam(required = false) String totalPrice,
                          @RequestParam(required = false) String priceNegotiation,
                          @RequestParam(required = false) String space,
                          @RequestParam(required = false) String floors,
                          @RequestParam(required = false) String ageEstate,
                          @RequestParam(required = false) String city,
                          @RequestParam(required = false) String districts,
                          @RequestParam(required = false) String address,
                          @RequestParam(required = false) String latitude,
                          @RequestParam(required = false) String longitude,
                          @RequestParam(required = false) String videoUrl,
                          @RequestParam(required = false) String arPath,
                          @RequestParam(required = false) String advertiserName,
                          @RequestParam(required = false) String phoneNumber,
                          RedirectAttributes redirectAttributes) {
        Estate estate = estateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estate not found: " + id));

        estate.setTitle(title);
        estate.setShortDescription(shortDescription);
        estate.setLongDescription(longDescription);
        estate.setAdvertisementType(advertisementType);
        estate.setEstateType(estateType);
        if (status != null && !status.isBlank()) {
            estate.setStatus(Estate.Status.valueOf(status));
        }
        estate.setPrice(price);
        estate.setTotalPrice(totalPrice);
        estate.setPriceNegotiation(priceNegotiation);
        estate.setSpace(space);
        estate.setFloors(parseIntOrNull(floors));
        estate.setAgeEstate(ageEstate);
        estate.setCity(city);
        estate.setDistricts(districts);
        estate.setAddress(address);
        estate.setLatitude(latitude);
        estate.setLongitude(longitude);
        estate.setVideoUrl(videoUrl);
        estate.setArPath(arPath);
        estate.setAdvertiserName(advertiserName);
        estate.setPhoneNumber(phoneNumber);
        estate.setUpdatedAt(LocalDateTime.now());

        estateRepository.save(estate);
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/estates/" + id + "/edit";
    }

    @PostMapping("/estates/{id}/upload-image")
    public String uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        Estate estate = estateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estate not found: " + id));

        R2StorageService.UploadResult result = r2StorageService.upload(file, "estate");
        if (result.success()) {
            List<String> images = new ArrayList<>(estate.getImageList());
            images.add(result.filename());
            try {
                estate.setImages(objectMapper.writeValueAsString(images));
            } catch (Exception ignored) {
                // keep the previous images value if serialization somehow fails
            }
            estate.setUpdatedAt(LocalDateTime.now());
            estateRepository.save(estate);
            redirectAttributes.addFlashAttribute("uploadResult", true);
        } else {
            redirectAttributes.addFlashAttribute("uploadResult", false);
            redirectAttributes.addFlashAttribute("uploadError", result.error());
        }
        return "redirect:/estates/" + id + "/edit";
    }

    private Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDoubleOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
