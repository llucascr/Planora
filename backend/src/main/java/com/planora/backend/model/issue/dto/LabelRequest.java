package com.planora.backend.model.issue.dto;

public record LabelRequest(
        String name,
        String description,
        String color
) {
}
