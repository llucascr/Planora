package com.planora.backend.model.issue.dto;

import com.planora.backend.model.issue.Issue;

import java.time.LocalDateTime;

public record IssueResponse(
        IssueApiResponse issueApiResponse,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt
) {

    public Issue toEntity() {
        Issue issue = issueApiResponse.toEntity();
        issue.setCreatedAt(this.createdAt);
        issue.setUpdatedAt(this.updatedAt);
        issue.setClosedAt(this.closedAt);
        return issue;
    }

}
