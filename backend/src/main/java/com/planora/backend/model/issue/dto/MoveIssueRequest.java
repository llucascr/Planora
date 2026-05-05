package com.planora.backend.model.issue.dto;

public record MoveIssueRequest(Long issueId, Long targetColumnId) {
}
