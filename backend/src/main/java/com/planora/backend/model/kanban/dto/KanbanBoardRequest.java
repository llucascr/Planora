package com.planora.backend.model.kanban.dto;

public record KanbanBoardRequest(
        String name,
        String description,
        String githubRepository
) {
}
