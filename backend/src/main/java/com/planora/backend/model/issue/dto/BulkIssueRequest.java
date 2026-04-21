package com.planora.backend.model.issue.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkIssueRequest(
        @NotEmpty @Valid List<IssueRequest> issues
) {
}
