package com.realestate.admin.controller.web;

import com.realestate.admin.entity.NotificationApp;
import com.realestate.admin.entity.Zone;
import com.realestate.admin.repository.CategoryRepository;
import com.realestate.admin.repository.NotificationAppRepository;
import com.realestate.admin.repository.ZoneRepository;
import com.realestate.admin.service.NotificationSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationAppRepository notificationAppRepository;
    private final ZoneRepository zoneRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationSendService notificationSendService;

    @GetMapping("/notifications")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<NotificationApp> result = notificationAppRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 10));

        Map<Long, String> zoneNames = new HashMap<>();
        for (Zone z : zoneRepository.findAll()) zoneNames.put(z.getId(), z.getNameAr());

        model.addAttribute("notifications", result);
        model.addAttribute("zoneNames", zoneNames);
        model.addAttribute("activePage", "notifications");
        return "notifications";
    }

    @GetMapping("/notifications/compose")
    public String composeForm(Model model) {
        model.addAttribute("zones", zoneRepository.findAll());
        model.addAttribute("categories", categoryRepository.findAllByOrderByPositionAsc());
        model.addAttribute("firebaseReady", notificationSendService.isFirebaseReady());
        model.addAttribute("activePage", "notifications");
        return "notification-compose";
    }

    @PostMapping("/notifications/send")
    public String send(@RequestParam String title,
                        @RequestParam String description,
                        @RequestParam(required = false) String zoneId,
                        @RequestParam(required = false) String categoryId,
                        @RequestParam(defaultValue = "all") String audience,
                        RedirectAttributes redirectAttributes) {

        Long zoneIdLong = parseLongOrNull(zoneId);
        Long categoryIdLong = parseLongOrNull(categoryId);

        NotificationSendService.SendResult result = notificationSendService.send(
                title, description, zoneIdLong, categoryIdLong, audience);

        // Log the attempt either way - this table is the notification history,
        // whether or not Firebase was actually configured to deliver it.
        NotificationApp record = new NotificationApp();
        record.setId(notificationAppRepository.findMaxId() + 1);
        record.setTitle(title);
        record.setDescription(description);
        record.setTarget(audience);
        record.setType(categoryIdLong != null ? "category:" + categoryIdLong : null);
        record.setZoneId(zoneIdLong);
        record.setStatus(result.sent());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        notificationAppRepository.save(record);

        redirectAttributes.addFlashAttribute("sendResult", result);
        return "redirect:/notifications";
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
