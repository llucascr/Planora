package com.planora.backend.model.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GithubCommitSearchResponse(
        @JsonProperty("total_count") int totalCount,
        List<GithubCommitItem> items
) {}
