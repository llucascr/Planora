package com.planora.backend.model.issue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubWebhookResponse(
        Long id,
        String url,
        String type,
        List<String> events,
        boolean active
) {}
