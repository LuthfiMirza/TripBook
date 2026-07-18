package com.tripbook.dto;

import java.time.LocalDateTime;

import com.tripbook.entity.User;

/** Never carries passwordHash — this is the only shape a User is allowed to leave the API in. */
public record UserResponse(Long id, String email, String fullName, String role, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getCreatedAt());
    }
}
