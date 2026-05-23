package com.planora.backend.model.issue.dto;

public record IssueAssigneeResponse(
        String login,
        String avatarUrl
) {
}