package com.planora.backend.model.issue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueWebhookPayload(
        String action,
        WebhookIssueData issue,
        WebhookRepositoryPayload repository
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebhookIssueData(
            Integer number,
            String title,
            String body,
            String state,
            String url,
            List<LabelResponse> labels,
            List<UserIssueResponse> assignees,
            @JsonProperty("created_at") String createdAt,
            @JsonProperty("updated_at") String updatedAt,
            @JsonProperty("closed_at") String closedAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebhookRepositoryPayload(
            String name,
            WebhookOwner owner
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record WebhookOwner(String login) {}
    }
}
