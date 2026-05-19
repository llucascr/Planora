package com.planora.backend.controller;

import com.planora.backend.model.dashboard.dto.ActivityDayEntry;
import com.planora.backend.model.dashboard.dto.CommitHistoryEntry;
import com.planora.backend.model.dashboard.dto.DashboardStatsResponse;
import com.planora.backend.model.dashboard.dto.MonthlyProgressEntry;
import com.planora.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(dashboardService.getStats(jwt));
    }

    @GetMapping("/activity")
    public ResponseEntity<List<ActivityDayEntry>> getActivityHistory(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(dashboardService.getActivityHistory(jwt));
    }

    @GetMapping("/commits")
    public ResponseEntity<List<CommitHistoryEntry>> getCommitHistory(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(dashboardService.getCommitHistory(jwt));
    }

    @GetMapping("/progress")
    public ResponseEntity<List<MonthlyProgressEntry>> getMonthlyProgress(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(dashboardService.getMonthlyProgress(jwt));
    }
}
