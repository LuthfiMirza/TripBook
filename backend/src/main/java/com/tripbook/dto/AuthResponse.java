package com.tripbook.dto;

public record AuthResponse(String token, UserResponse user) {
}
