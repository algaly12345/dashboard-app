package com.realestate.admin.controller.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes the current request path available to every Thymeleaf template so the
 * language switch links (?lang=ar / ?lang=en) can point back at the same page.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        String query = request.getQueryString();
        String path = request.getRequestURI();
        if (query == null) return path;
        // strip any existing lang param so the switch link doesn't stack "lang=ar&lang=en"
        String cleaned = query.replaceAll("(?:^|&)lang=[^&]*", "").replaceFirst("^&", "");
        return cleaned.isBlank() ? path : path + "?" + cleaned;
    }
}
