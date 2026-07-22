package com.realestate.admin.controller.web;

import com.realestate.admin.entity.Estate;
import com.realestate.admin.repository.EstateRepository;
import com.realestate.admin.service.AdminPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;
import java.util.List;

/**
 * Makes data every page's shared fragments need (topbar, sidebar) available
 * globally, so individual controllers don't each have to wire it up:
 * the current request path (language switch links), the signed-in admin's
 * name (profile icon), and a "new listings" count + preview for the
 * notification bell.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final EstateRepository estateRepository;

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        String query = request.getQueryString();
        String path = request.getRequestURI();
        if (query == null) return path;
        // strip any existing lang param so the switch link doesn't stack "lang=ar&lang=en"
        String cleaned = query.replaceAll("(?:^|&)lang=[^&]*", "").replaceFirst("^&", "");
        return cleaned.isBlank() ? path : path + "?" + cleaned;
    }

    @ModelAttribute("currentAdminName")
    public String currentAdminName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AdminPrincipal principal) {
            return principal.getAdmin().getName();
        }
        return null;
    }

    @ModelAttribute("navNewEstatesCount")
    public long navNewEstatesCount() {
        return estateRepository.countByCreatedAtGreaterThanEqual(LocalDate.now().atStartOfDay());
    }

    @ModelAttribute("navRecentEstates")
    public List<Estate> navRecentEstates() {
        return estateRepository.findTop6ByOrderByCreatedAtDesc();
    }
}
