package com.planora.backend.model.kanban.dto;

import java.time.LocalDateTime;
import java.util.List;

public record KanbanBoardResponse(
        Long kanbanBoardId,
        String name,
        String description,
        String githubRepository,
        String githubOwnerName,
        String ownerLogin,
        LocalDateTime createdAt,
        List<KanbanColumnResponse> columns,
        List<KanbanMemberResponse> members
) {
}
