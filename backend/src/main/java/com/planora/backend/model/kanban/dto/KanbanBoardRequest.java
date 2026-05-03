package com.planora.backend.model.kanban.dto;

import java.util.List;

public record KanbanBoardRequest(
        String name,
        String description,
        String githubRepository,
        List<KanbanColumnRequest> columns
) {
}
