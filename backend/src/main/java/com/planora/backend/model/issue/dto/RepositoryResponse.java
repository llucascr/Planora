package com.planora.backend.model.issue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RepositoryResponse(
        String id,
        String name,
        String full_name,
        String is_private
) {
}
