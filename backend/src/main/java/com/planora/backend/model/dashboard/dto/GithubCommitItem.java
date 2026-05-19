package com.planora.backend.model.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubCommitItem(
        String sha,
        @JsonProperty("html_url") String htmlUrl,
        CommitDetails commit,
        CommitRepository repository
) {
    public record CommitDetails(
            String message,
            CommitAuthor committer
    ) {}

    public record CommitAuthor(
            String name,
            String date
    ) {}

    public record CommitRepository(
            String name,
            @JsonProperty("full_name") String fullName
    ) {}
}
