package com.planora.backend.model.issue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.planora.backend.model.issue.Label;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LabelResponse(
        String url,
        String name,
        String color,
        String description
) {
    public static LabelResponse fromEntity(Label label) {
        return new LabelResponse(
                label.getUrl(),
                label.getName(),
                label.getColor(),
                label.getDescription()
        );
    }
}
