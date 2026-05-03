package com.planora.backend.model.kanban.dto;

public record KanbanColumnRequest(
        Long kanbanColumnId,
        String name,
        Integer position
) {}