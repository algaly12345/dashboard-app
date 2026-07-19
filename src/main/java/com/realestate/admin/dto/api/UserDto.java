package com.realestate.admin.dto.api;

import com.realestate.admin.entity.AppUser;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String name,
        String phone,
        String email,
        String userType,
        String membershipType,
        String isActive,
        LocalDateTime createdAt
) {
    public static UserDto from(AppUser u) {
        return new UserDto(
                u.getId(), u.getName(), u.getPhone(), u.getEmail(), u.getUserType(),
                u.getMembershipType(), u.getIsActive() != null ? u.getIsActive().name() : null,
                u.getCreatedAt());
    }
}
