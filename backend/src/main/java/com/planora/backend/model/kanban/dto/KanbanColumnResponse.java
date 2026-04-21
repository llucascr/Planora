package com.planora.backend.model.kanban.dto;

public record KanbanColumnResponse(
        Long kanbanColumnId,
        String name,
        Integer position
) {
}
