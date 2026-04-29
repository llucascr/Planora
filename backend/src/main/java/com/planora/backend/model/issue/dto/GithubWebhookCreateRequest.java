package com.planora.backend.model.issue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GithubWebhookCreateRequest(
        String name,
        GithubWebhookConfig config,
        List<String> events,
        boolean active
) {
    public record GithubWebhookConfig(
            String url,
            @JsonProperty("content_type") String contentType,
            String secret,
            @JsonProperty("insecure_ssl") String insecureSsl
    ) {}
}
