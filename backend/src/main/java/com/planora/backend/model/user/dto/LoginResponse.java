package com.planora.backend.model.user.dto;

public record LoginResponse(
        String accessToken,
        Long expiresIn
) {
}
