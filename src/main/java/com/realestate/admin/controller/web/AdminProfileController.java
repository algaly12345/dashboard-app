package com.realestate.admin.controller.web;

import com.realestate.admin.entity.Admin;
import com.realestate.admin.repository.AdminRepository;
import com.realestate.admin.service.AdminPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class AdminProfileController {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public String view(Model model) {
        model.addAttribute("admin", currentAdmin());
        model.addAttribute("activePage", "profile");
        return "profile";
    }

    @PostMapping("/profile")
    public String update(@RequestParam String name,
                          @RequestParam String email,
                          @RequestParam String phone,
                          @RequestParam(required = false) String newPassword,
                          @RequestParam(required = false) String confirmPassword,
                          RedirectAttributes redirectAttributes) {
        Admin admin = currentAdmin();

        admin.setName(name);
        admin.setEmail(email);
        admin.setPhone(phone);

        if (newPassword != null && !newPassword.isBlank()) {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("profileError", "password_mismatch");
                return "redirect:/profile";
            }
            admin.setPassword(passwordEncoder.encode(newPassword));
        }

        admin.setUpdatedAt(LocalDateTime.now());
        adminRepository.save(admin);

        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/profile";
    }

    private Admin currentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AdminPrincipal principal) {
            // re-fetch so we always work with a fresh, managed entity
            return adminRepository.findById(principal.getAdmin().getId())
                    .orElseThrow(() -> new IllegalStateException("Signed-in admin no longer exists"));
        }
        throw new IllegalStateException("No authenticated admin in context");
    }
}
