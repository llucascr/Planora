package com.planora.backend.model.dashboard.dto;

public record CommitHistoryEntry(
        String sha,
        String message,
        String date,
        String repositoryName,
        String url
) {}
