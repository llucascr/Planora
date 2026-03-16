package com.planora.backend.model.issue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserIssueResponse(
        String login,
        @JsonProperty("avatar_url") String avatarUrl,
        String email,
        @JsonProperty("notification_email") String notificationEmail
) {}
