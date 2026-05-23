package com.planora.backend.model.dashboard.dto;

import java.util.List;

public record DashboardOverviewResponse(
        int totalCommitsLast30Days,
        int mergedPRsLast30Days,
        int openPRsCount,
        List<ActivityDayEntry> activityHistory,
        List<CommitHistoryEntry> commitHistory,
        long activeBoardsCount,
        long assignedIssuesLast30Days
) {}
