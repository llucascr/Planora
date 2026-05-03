package com.planora.backend.model.kanban.dto;

import com.planora.backend.model.issue.dto.IssueSummaryResponse;

import java.util.List;

public record KanbanColumnWithIssuesResponse(
        Long kanbanColumnId,
        String name,
        Integer position,
        List<IssueSummaryResponse> issues
) {
}
