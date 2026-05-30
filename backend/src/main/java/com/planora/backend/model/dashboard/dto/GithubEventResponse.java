package com.planora.backend.model.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubEventResponse(
        String type,
        @JsonProperty("created_at") String createdAt,
        EventRepo repo,
        EventPayload payload
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventRepo(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventPayload(
            List<EventCommit> commits
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventCommit(
            String sha,
            String message,
            String url
    ) {}
}
