package com.planora.backend.model.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.planora.backend.model.user.Role;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        String login,
        String avatarUrl,
        String email,
        String notificationEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Set<Role> roles
) {}
