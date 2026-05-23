package com.planora.backend.model.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubSearchResponse(
        @JsonProperty("total_count") int totalCount
) {}
