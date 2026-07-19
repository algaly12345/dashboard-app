package com.realestate.admin.controller.api;

import com.realestate.admin.dto.EstateMapPin;
import com.realestate.admin.dto.api.EstateDetailDto;
import com.realestate.admin.dto.api.EstateSummaryDto;
import com.realestate.admin.entity.Estate;
import com.realestate.admin.repository.EstateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * First slice of a REST API for this back office. Kept deliberately small
 * (read-only, DTO-shaped) so a future mobile app / external client / write
 * endpoints can be layered on without touching the Thymeleaf controllers in
 * {@code controller.web}. All endpoints currently sit behind the same
 * session-based admin login as the dashboard (see SecurityConfig) - swap in
 * a stateless auth scheme (API key / JWT) here first if this is ever opened
 * up to non-admin clients.
 */
@RestController
@RequestMapping("/api/v1/estates")
@RequiredArgsConstructor
public class EstateApiController {

    private final EstateRepository estateRepository;

    @GetMapping
    public Page<EstateSummaryDto> list(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) String city,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        Estate.Status statusEnum = (status != null && !status.isBlank()) ? Estate.Status.valueOf(status) : null;
        Page<Estate> result = estateRepository.search(
                blankToNull(q), statusEnum, blankToNull(city), blankToNull(category),
                null, null, null, null, null,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return result.map(EstateSummaryDto::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstateDetailDto> get(@PathVariable Long id) {
        return estateRepository.findById(id)
                .map(EstateDetailDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Powers the jQuery map view: paginated pins matching zone/city/district/category/advertiser. */
    @GetMapping("/map")
    public Page<EstateMapPin> map(@RequestParam(required = false) String zoneId,
                                   @RequestParam(required = false) String city,
                                   @RequestParam(required = false) String district,
                                   @RequestParam(required = false) String categoryId,
                                   @RequestParam(required = false) String advertiserName,
                                   @RequestParam(defaultValue = "0") int page) {
        Page<Estate> result = estateRepository.searchForMap(
                parseLong(zoneId), blankToNull(city), blankToNull(district), parseInt(categoryId),
                blankToNull(advertiserName),
                PageRequest.of(page, 12, Sort.by(Sort.Direction.DESC, "createdAt")));
        return result.map(EstateMapPin::from);
    }

    /** Cascading district dropdown - districts narrow down to whichever city is selected. */
    @GetMapping("/districts")
    public List<String> districts(@RequestParam(required = false) String city) {
        return estateRepository.findDistinctDistricts(blankToNull(city));
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private Integer parseInt(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
