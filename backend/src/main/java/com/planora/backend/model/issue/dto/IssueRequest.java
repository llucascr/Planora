package com.planora.backend.model.issue.dto;

import com.planora.backend.model.user.dto.UserResponse;

import java.util.List;

public record IssueRequest(
        String title,
        String body,
        List<String> assignees,
        List<String> labels
) {
}
