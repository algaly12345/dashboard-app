package com.realestate.admin.controller.web;

import com.realestate.admin.entity.AppUser;
import com.realestate.admin.entity.Estate;
import com.realestate.admin.repository.AppUserRepository;
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
    private final AppUserRepository appUserRepository;
    private final com.realestate.admin.service.ImageUrlService imageUrlService;
    private final com.realestate.admin.repository.ZoneRepository zoneRepository;
    private final com.realestate.admin.repository.RegionLiteRepository regionLiteRepository;
    private final com.realestate.admin.repository.CityLiteRepository cityLiteRepository;
    private final com.realestate.admin.repository.DistrictLiteRepository districtLiteRepository;
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
                    @RequestParam(required = false) String licenseExpired,
                    @RequestParam(required = false) String zoneId,
                    @RequestParam(required = false) String userId,
                    @RequestParam(defaultValue = "0") int page,
                    Model model) {

    Estate.Status statusEnum = (status != null && !status.isBlank()) ? Estate.Status.valueOf(status) : null;
    Boolean virtualTourBool = (virtualTour == null || virtualTour.isBlank()) ? null : "yes".equals(virtualTour);
    Double minPriceVal = parseDoubleOrNull(minPrice);
    Double maxPriceVal = parseDoubleOrNull(maxPrice);
    Long zoneIdLong = parseLongOrNull(zoneId);
    Long userIdLong = parseLongOrNull(userId);
    boolean expiredOnly = "true".equals(licenseExpired);

    Page<Estate> result;
    if (expiredOnly) {
        List<Estate> expired = estateRepository.findExpiredLicenses(zoneIdLong, PageRequest.of(page, 12));
        long total = estateRepository.countExpiredLicenses(zoneIdLong);
        result = new org.springframework.data.domain.PageImpl<>(expired, PageRequest.of(page, 12), total);
    } else {
        result = estateRepository.search(
                blankToNull(q), statusEnum, blankToNull(city), blankToNull(category), blankToNull(adType),
                blankToNull(estateType), virtualTourBool, minPriceVal, maxPriceVal, zoneIdLong, userIdLong,
                PageRequest.of(page, 12, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    List<String> cities = estateRepository.findDistinctCitiesByZone(zoneIdLong);
    List<String> categories = estateRepository.findDistinctCategoryNames();
    List<String> estateTypes = estateRepository.findDistinctEstateTypes();

    // ---- Advertisers shown on this page: resolved photo URL + estate count each ----
    List<Long> userIds = result.getContent().stream()
            .map(Estate::getUserId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();

    java.util.Map<Long, com.realestate.admin.dto.AdvertiserInfo> estateUsers = new java.util.HashMap<>();
    for (AppUser u : appUserRepository.findAllById(userIds)) {
        long estateCount = estateRepository.countByUserId(u.getId());
        String imageUrl = (u.getImage() != null && !u.getImage().isBlank())
                ? imageUrlService.profileImage(u.getImage()) : null;
        estateUsers.put(u.getId(), new com.realestate.admin.dto.AdvertiserInfo(
                u.getId(), u.getName(), u.getPhone(), u.getEmail(), u.getUnifiedNumber(),
                u.getAdvertiserNo(), u.getFalLicenseNumber(), imageUrl,
                u.getYoutube(), u.getSnapchat(), u.getInstagram(), u.getWebsite(), u.getTiktok(), u.getTwitter(),
                estateCount
        ));
    }

    model.addAttribute("estates", result);
    model.addAttribute("estateUsers", estateUsers);
    model.addAttribute("cities", cities);
    model.addAttribute("regionsLite", regionLiteRepository.findAllOrderByName());
    model.addAttribute("zones", zoneRepository.findAll());
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
    model.addAttribute("licenseExpired", licenseExpired);
    model.addAttribute("zoneId", zoneId);
    model.addAttribute("expiredOnly", expiredOnly);
    model.addAttribute("activePage", "estates");

    List<Long> userIdsWithEstates = estateRepository.findDistinctUserIds();
    List<com.realestate.admin.entity.AppUser> usersWithEstates = appUserRepository.findAllById(userIdsWithEstates);
    model.addAttribute("usersWithEstates", usersWithEstates);
    if (userIdLong != null) {
        usersWithEstates.stream()
                .filter(u -> u.getId().equals(userIdLong))
                .findFirst()
                .ifPresent(u -> model.addAttribute("selectedAdvertiserLabel", u.getName() + " — " + u.getPhone()));
    }

    return "estates";
}

@GetMapping("/estates/cities-by-region")
@ResponseBody
public List<String> citiesByRegion(@RequestParam Integer regionId) {
    return cityLiteRepository.findByRegionIdOrderByNameAr(regionId).stream()
            .map(com.realestate.admin.entity.CityLite::getNameAr)
            .toList();
}

@GetMapping("/estates/districts-by-city")
@ResponseBody
public List<String> districtsByCity(@RequestParam String cityName) {
    java.util.Optional<com.realestate.admin.entity.CityLite> match = cityLiteRepository.findAllByNameAr(cityName).stream().findFirst();
    if (match.isEmpty()) return List.of();
    return districtLiteRepository.findByCityIdOrderByNameAr(match.get().getCityId()).stream()
            .map(com.realestate.admin.entity.DistrictLite::getNameAr)
            .toList();
}

@GetMapping("/estates/advertisers-search")
@ResponseBody
public List<java.util.Map<String, Object>> advertisersSearch(@RequestParam(required = false) String q) {
    String term = (q == null) ? "" : q.trim().toLowerCase();
    List<Long> userIdsWithEstates = estateRepository.findDistinctUserIds();
    return appUserRepository.findAllById(userIdsWithEstates).stream()
            .filter(u -> term.isEmpty()
                    || (u.getName() != null && u.getName().toLowerCase().contains(term))
                    || (u.getPhone() != null && u.getPhone().contains(term)))
            .limit(20)
            .map(u -> {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", u.getId());
                m.put("name", u.getName());
                m.put("phone", u.getPhone());
                return m;
            })
            .toList();
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
public String editForm(@PathVariable Long id,
                        @RequestParam(required = false) Long previewUserId,
                        Model model) {
    Estate estate = estateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Estate not found: " + id));
    model.addAttribute("estate", estate);
    model.addAttribute("users", appUserRepository.findAll());

    Long userIdToShow = previewUserId != null ? previewUserId : estate.getUserId();
    if (userIdToShow != null) {
        appUserRepository.findById(userIdToShow).ifPresent(u -> model.addAttribute("selectedUser", u));
    }

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

    @GetMapping("/estates/{id}/photos")
    public String photos(@PathVariable Long id, Model model) {
        Estate estate = estateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estate not found: " + id));
        model.addAttribute("estate", estate);
        model.addAttribute("activePage", "estates");
        return "estate-photos";
    }

@PostMapping("/estates/{id}/upload-image")
public String uploadImage(@PathVariable Long id, @RequestParam("files") MultipartFile[] files,
                           @RequestParam(required = false) String redirectTo,
                           RedirectAttributes redirectAttributes) {
    Estate estate = estateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Estate not found: " + id));

    List<String> images = new ArrayList<>(estate.getImageList());
    int successCount = 0;
    String lastError = null;

    for (MultipartFile file : files) {
        if (file.isEmpty()) continue;
        R2StorageService.UploadResult result = r2StorageService.upload(file, "estate");
        if (result.success()) {
            images.add(result.filename());
            successCount++;
        } else {
            lastError = result.error();
        }
    }

    if (successCount > 0) {
        try {
            estate.setImages(objectMapper.writeValueAsString(images));
        } catch (Exception ignored) {
        }
        estate.setUpdatedAt(LocalDateTime.now());
        estateRepository.save(estate);
        redirectAttributes.addFlashAttribute("uploadResult", true);
    } else {
        redirectAttributes.addFlashAttribute("uploadResult", false);
        redirectAttributes.addFlashAttribute("uploadError", lastError);
    }
    return "redirect:" + (redirectTo != null && !redirectTo.isBlank() ? redirectTo : "/estates/" + id + "/edit");
}
    @PostMapping("/estates/{id}/delete-image")
    public String deleteImage(@PathVariable Long id, @RequestParam("filename") String filename,
                               @RequestParam(required = false) String redirectTo,
                               RedirectAttributes redirectAttributes) {
        Estate estate = estateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estate not found: " + id));

        List<String> images = new ArrayList<>(estate.getImageList());
        images.remove(filename);
        try {
            estate.setImages(objectMapper.writeValueAsString(images));
            estate.setUpdatedAt(LocalDateTime.now());
            estateRepository.save(estate);
        } catch (Exception ignored) {
            // keep the previous images value if serialization somehow fails
        }
        // Note: this only unlinks the photo from the listing - it doesn't
        // delete the underlying object from R2, so nothing else that might
        // still reference the same file (a shared/reused image) breaks.
        return "redirect:" + (redirectTo != null && !redirectTo.isBlank() ? redirectTo : "/estates/" + id + "/edit");
    }

    @PostMapping("/estates/{id}/upload-plan")
    public String uploadPlan(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                              @RequestParam(required = false) String redirectTo,
                              RedirectAttributes redirectAttributes) {
        Estate estate = estateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estate not found: " + id));

        R2StorageService.UploadResult result = r2StorageService.upload(file, "estate");
        if (result.success()) {
            List<String> planned = new ArrayList<>(estate.getPlannedList());
            planned.add(result.filename());
            try {
                estate.setPlanned(objectMapper.writeValueAsString(planned));
            } catch (Exception ignored) {
                // keep the previous value if serialization somehow fails
            }
            estate.setUpdatedAt(LocalDateTime.now());
            estateRepository.save(estate);
            redirectAttributes.addFlashAttribute("uploadResult", true);
        } else {
            redirectAttributes.addFlashAttribute("uploadResult", false);
            redirectAttributes.addFlashAttribute("uploadError", result.error());
        }
        return "redirect:" + (redirectTo != null && !redirectTo.isBlank() ? redirectTo : "/estates/" + id + "/photos");
    }

    @PostMapping("/estates/{id}/delete-plan")
    public String deletePlan(@PathVariable Long id, @RequestParam("filename") String filename,
                              @RequestParam(required = false) String redirectTo) {
        Estate estate = estateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estate not found: " + id));

        List<String> planned = new ArrayList<>(estate.getPlannedList());
        planned.remove(filename);
        try {
            estate.setPlanned(objectMapper.writeValueAsString(planned));
            estate.setUpdatedAt(LocalDateTime.now());
            estateRepository.save(estate);
        } catch (Exception ignored) {
            // keep the previous value if serialization somehow fails
        }
        return "redirect:" + (redirectTo != null && !redirectTo.isBlank() ? redirectTo : "/estates/" + id + "/photos");
    }

    @PostMapping("/estates/{id}/upload-video")
    public String uploadVideo(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                               @RequestParam(required = false) String redirectTo,
                               RedirectAttributes redirectAttributes) {
        Estate estate = estateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estate not found: " + id));

        R2StorageService.UploadResult result = r2StorageService.upload(file, "videos");
        if (result.success()) {
            estate.setVideoUrl(result.filename());
            estate.setUpdatedAt(LocalDateTime.now());
            estateRepository.save(estate);
            redirectAttributes.addFlashAttribute("uploadResult", true);
        } else {
            redirectAttributes.addFlashAttribute("uploadResult", false);
            redirectAttributes.addFlashAttribute("uploadError", result.error());
        }
        return "redirect:" + (redirectTo != null && !redirectTo.isBlank() ? redirectTo : "/estates/" + id + "/photos");
    }

    @PostMapping("/estates/{id}/upload-skyview")
    public String uploadSkyview(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                 @RequestParam(required = false) String redirectTo,
                                 RedirectAttributes redirectAttributes) {
        Estate estate = estateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estate not found: " + id));

        R2StorageService.UploadResult result = r2StorageService.upload(file, "videos");
        if (result.success()) {
            estate.setSkyview(result.filename());
            estate.setUpdatedAt(LocalDateTime.now());
            estateRepository.save(estate);
            redirectAttributes.addFlashAttribute("uploadResult", true);
        } else {
            redirectAttributes.addFlashAttribute("uploadResult", false);
            redirectAttributes.addFlashAttribute("uploadError", result.error());
        }
        return "redirect:" + (redirectTo != null && !redirectTo.isBlank() ? redirectTo : "/estates/" + id + "/photos");
    }

    @PostMapping("/estates/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        estateRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("deleted", true);
        return "redirect:/estates";
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


    @PostMapping("/estates/{id}/upload-user-photo")
public String uploadUserPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                               @RequestParam Long userId,
                               RedirectAttributes redirectAttributes) {
    AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    R2StorageService.UploadResult result = r2StorageService.upload(file, "profile");
    if (result.success()) {
        user.setImage(result.filename());
        user.setUpdatedAt(LocalDateTime.now());
        appUserRepository.save(user);
        redirectAttributes.addFlashAttribute("uploadResult", true);
    } else {
        redirectAttributes.addFlashAttribute("uploadResult", false);
        redirectAttributes.addFlashAttribute("uploadError", result.error());
    }
    return "redirect:/estates/" + id + "/edit?previewUserId=" + userId;
}



@PostMapping("/estates/{id}/update-user")
public String updateUser(@PathVariable Long id,
                          @RequestParam Long userId,
                          @RequestParam(required = false) String userName,
                          @RequestParam(required = false) String userPhone,
                          @RequestParam(required = false) String userEmail,
                          @RequestParam(required = false) String userUnifiedNumber,
                          @RequestParam(required = false) Integer userAdvertiserNo,
                          @RequestParam(required = false) String userYoutube,
                          @RequestParam(required = false) String userSnapchat,
                          @RequestParam(required = false) String userInstagram,
                          @RequestParam(required = false) String userWebsite,
                          @RequestParam(required = false) String userTiktok,
                          @RequestParam(required = false) String userTwitter,
                          RedirectAttributes redirectAttributes) {

    AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    user.setName(userName);
    user.setPhone(userPhone);
    user.setEmail(userEmail);
    user.setUnifiedNumber(userUnifiedNumber);
    user.setAdvertiserNo(userAdvertiserNo);
    user.setYoutube(userYoutube);
    user.setSnapchat(userSnapchat);
    user.setInstagram(userInstagram);
    user.setWebsite(userWebsite);
    user.setTiktok(userTiktok);
    user.setTwitter(userTwitter);
    user.setUpdatedAt(LocalDateTime.now());
    appUserRepository.save(user);

    Estate estate = estateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Estate not found: " + id));
    estate.setUserId(userId);
    estate.setUpdatedAt(LocalDateTime.now());
    estateRepository.save(estate);

    redirectAttributes.addFlashAttribute("userSaved", true);
    return "redirect:/estates/" + id + "/edit?previewUserId=" + userId;
}
}
