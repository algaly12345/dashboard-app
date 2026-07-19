package com.realestate.admin.controller.api;

import com.realestate.admin.dto.api.UserDto;
import com.realestate.admin.entity.AppUser;
import com.realestate.admin.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController {

    private final AppUserRepository appUserRepository;

    @GetMapping
    public Page<UserDto> list(@RequestParam(required = false) String q,
                               @RequestParam(required = false) String userType,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        Page<AppUser> result = appUserRepository.search(
                blankToNull(q), blankToNull(userType), PageRequest.of(page, size));
        return result.map(UserDto::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> get(@PathVariable Long id) {
        return appUserRepository.findById(id)
                .map(UserDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
