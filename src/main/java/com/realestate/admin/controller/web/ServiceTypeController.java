package com.realestate.admin.controller.web;

import com.realestate.admin.entity.ServiceType;
import com.realestate.admin.repository.OfferRepository;
import com.realestate.admin.repository.ServiceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ServiceTypeController {

    private final ServiceTypeRepository serviceTypeRepository;
    private final OfferRepository offerRepository;

    @GetMapping("/service-types")
    public String list(Model model) {
        List<ServiceType> types = serviceTypeRepository.findAllByOrderByNameAsc();

        Map<Long, Long> offerCounts = new HashMap<>();
        for (Object[] row : offerRepository.countGroupedByServiceType()) {
            offerCounts.put((Long) row[0], (Long) row[1]);
        }

        model.addAttribute("serviceTypes", types);
        model.addAttribute("offerCounts", offerCounts);
        model.addAttribute("activePage", "service-types");
        return "service-types";
    }

    @PostMapping("/service-types")
    public String create(@RequestParam String name, RedirectAttributes redirectAttributes) {
        ServiceType type = new ServiceType();
        type.setId(serviceTypeRepository.findMaxId() + 1);
        type.setName(name);
        type.setCreatedAt(LocalDateTime.now());
        type.setUpdatedAt(LocalDateTime.now());
        serviceTypeRepository.save(type);
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/service-types";
    }

    @PostMapping("/service-types/{id}")
    public String update(@PathVariable Long id, @RequestParam String name, RedirectAttributes redirectAttributes) {
        serviceTypeRepository.findById(id).ifPresent(type -> {
            type.setName(name);
            type.setUpdatedAt(LocalDateTime.now());
            serviceTypeRepository.save(type);
        });
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/service-types";
    }

    @PostMapping("/service-types/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        long inUse = offerRepository.countByServiceTypeId(id);
        if (inUse > 0) {
            redirectAttributes.addFlashAttribute("deleteError", inUse);
        } else {
            serviceTypeRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("deleted", true);
        }
        return "redirect:/service-types";
    }
}
