package com.planora.backend.model.issue.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IssueUpdateRequest(
        String title,
        String body,
        String state,
        List<String> labels,
        List<String> assignees
) {}
