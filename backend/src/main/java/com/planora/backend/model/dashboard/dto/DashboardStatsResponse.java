package com.planora.backend.model.dashboard.dto;

public record DashboardStatsResponse(
        int totalCommitsLast30Days,
        int mergedPRsLast30Days,
        int openPRsCount,
        long activeBoardsCount,
        long assignedIssuesLast30Days
) {}
