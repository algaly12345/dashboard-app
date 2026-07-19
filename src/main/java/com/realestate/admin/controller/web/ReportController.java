package com.realestate.admin.controller.web;

import com.realestate.admin.entity.AppUser;
import com.realestate.admin.entity.Estate;
import com.realestate.admin.entity.Report;
import com.realestate.admin.repository.AppUserRepository;
import com.realestate.admin.repository.EstateRepository;
import com.realestate.admin.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ReportController {

    private final ReportRepository reportRepository;
    private final EstateRepository estateRepository;
    private final AppUserRepository appUserRepository;

    @GetMapping("/reports")
    public String list(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String title,
                        @RequestParam(required = false) String estateId,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {

        Long estateIdLong = parseLongOrNull(estateId);

        Page<Report> result = reportRepository.search(
                blankToNull(q), blankToNull(title), estateIdLong,
                PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        // Batch-resolve the estates and reporter accounts referenced on this
        // page only - avoids one query per row.
        List<Long> estateIds = result.getContent().stream().map(Report::getEstateId).distinct().toList();
        Map<Long, Estate> estates = estateRepository.findAllById(estateIds).stream()
                .collect(Collectors.toMap(Estate::getId, e -> e));

        List<Long> userIds = result.getContent().stream()
                .map(Report::getReporterId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, AppUser> reporters = new HashMap<>();
        for (AppUser u : appUserRepository.findAllById(userIds)) {
            reporters.put(u.getId(), u);
        }

        List<String> titles = reportRepository.findDistinctTitles();

        model.addAttribute("reports", result);
        model.addAttribute("estates", estates);
        model.addAttribute("reporters", reporters);
        model.addAttribute("titles", titles);
        model.addAttribute("q", q);
        model.addAttribute("title", title);
        model.addAttribute("estateId", estateId);
        model.addAttribute("activePage", "reports");

        return "reports";
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
