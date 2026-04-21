package com.planora.backend.model.issue.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record IssueRequest(
        @NotBlank String title,
        String body,
        List<String> assignees,
        List<String> labels
) {
}
