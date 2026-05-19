package com.planora.backend.model.dashboard.dto;

public record MonthlyProgressEntry(
        String date,
        long opened,
        long closed
) {}
