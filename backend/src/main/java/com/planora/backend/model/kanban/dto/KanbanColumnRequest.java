package com.planora.backend.model.kanban.dto;

public record KanbanColumnRequest(
        String name,
        Integer position
) {
}
