package com.planora.backend.model.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.planora.backend.model.user.Role;

import java.time.LocalDateTime;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserResponse(
        String login,
        @JsonProperty("avatar_url") String avatarUrl,
        String email,
        @JsonProperty("notification_email") String notificationEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Set<Role> roles
) {}
