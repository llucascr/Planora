package com.planora.backend.service;

import com.planora.backend.client.GithubClient;
import com.planora.backend.model.dashboard.dto.ActivityDayEntry;
import com.planora.backend.model.dashboard.dto.CommitHistoryEntry;
import com.planora.backend.model.dashboard.dto.DashboardStatsResponse;
import com.planora.backend.model.dashboard.dto.GithubCommitSearchResponse;
import com.planora.backend.model.dashboard.dto.GithubEventResponse;
import com.planora.backend.model.dashboard.dto.MonthlyProgressEntry;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.IssueRepository;
import com.planora.backend.repository.KanbanBoardRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final String GITHUB_API_VERSION = "2022-11-28";
    private static final String COMMITS_ACCEPT_HEADER = "application/vnd.github.cloak-preview+json";
    private static final int COMMIT_HISTORY_LIMIT = 200;

    private final GithubClient githubClient;
    private final UserService userService;
    private final TokenService tokenService;
    private final IssueRepository issueRepository;
    private final KanbanBoardRepository kanbanBoardRepository;

    public DashboardStatsResponse getStats(Jwt jwt) {
        Long userId = tokenService.getUserId(jwt);
        String githubToken = "Bearer " + tokenService.getGithubToken(jwt);
        User user = userService.findById(userId);
        String login = user.getLogin();

        LocalDateTime since = LocalDateTime.now().minusDays(30);
        String sinceDate = since.toLocalDate().toString();

        GithubCommitSearchResponse commitSearch = fetchCommitSearch(githubToken, login, sinceDate);
        int mergedPRs = fetchMergedPRsLast30Days(githubToken, login, sinceDate);
        int openPRs = fetchOpenPRsCount(githubToken, login);
        long activeBoards = kanbanBoardRepository.countActiveBoardsSince(userId, since);
        long assignedIssues = issueRepository.countAssignedIssuesSince(userId, since);

        return new DashboardStatsResponse(
                commitSearch.totalCount(),
                mergedPRs,
                openPRs,
                activeBoards,
                assignedIssues
        );
    }

    public List<ActivityDayEntry> getActivityHistory(Jwt jwt) {
        String githubToken = "Bearer " + tokenService.getGithubToken(jwt);
        User user = userService.findById(tokenService.getUserId(jwt));
        return fetchActivityHistory(githubToken, user.getLogin());
    }

    public List<CommitHistoryEntry> getCommitHistory(Jwt jwt) {
        String githubToken = "Bearer " + tokenService.getGithubToken(jwt);
        User user = userService.findById(tokenService.getUserId(jwt));
        String sinceDate = LocalDateTime.now().minusDays(60).toLocalDate().toString();
        return mapCommitHistory(fetchCommitSearch(githubToken, user.getLogin(), sinceDate));
    }

    private List<CommitHistoryEntry> mapCommitHistory(GithubCommitSearchResponse response) {
        if (response.items() == null) return Collections.emptyList();
        return response.items().stream()
                .map(item -> new CommitHistoryEntry(
                        item.sha().substring(0, 7),
                        extractFirstLine(item.commit().message()),
                        item.commit().committer().date(),
                        item.repository() != null ? item.repository().fullName() : "",
                        item.htmlUrl()
                ))
                .toList();
    }

    public List<MonthlyProgressEntry> getMonthlyProgress(Jwt jwt) {
        Long userId = tokenService.getUserId(jwt);
        LocalDateTime since = LocalDateTime.now().minusDays(30);

        Map<String, long[]> progressByDay = new HashMap<>();

        issueRepository.findAssignedIssuesCreatedSince(userId, since)
                .forEach(issue -> {
                    String day = issue.getCreatedAt().toLocalDate().toString();
                    progressByDay.computeIfAbsent(day, k -> new long[]{0, 0})[0]++;
                });

        issueRepository.findAssignedIssuesClosedSince(userId, since)
                .forEach(issue -> {
                    String day = issue.getClosedAt().toLocalDate().toString();
                    progressByDay.computeIfAbsent(day, k -> new long[]{0, 0})[1]++;
                });

        return progressByDay.entrySet().stream()
                .map(e -> new MonthlyProgressEntry(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparing(MonthlyProgressEntry::date))
                .toList();
    }

    private GithubCommitSearchResponse fetchCommitSearch(String bearerToken, String login, String sinceDate) {
        try {
            String query = "author:" + login + " committer-date:>=" + sinceDate;
            return githubClient.searchCommits(bearerToken, GITHUB_API_VERSION, COMMITS_ACCEPT_HEADER, query, COMMIT_HISTORY_LIMIT, "committer-date", "desc");
        } catch (Exception e) {
            log.warn("Failed to fetch commits for {}: {}", login, e.getMessage());
            return new GithubCommitSearchResponse(0, Collections.emptyList());
        }
    }

    private int fetchMergedPRsLast30Days(String bearerToken, String login, String sinceDate) {
        try {
            String query = "author:" + login + " is:pr is:merged merged:>=" + sinceDate;
            return githubClient.searchIssues(bearerToken, GITHUB_API_VERSION, query, 1).totalCount();
        } catch (Exception e) {
            log.warn("Failed to fetch merged PR count for {}: {}", login, e.getMessage());
            return 0;
        }
    }

    private int fetchOpenPRsCount(String bearerToken, String login) {
        try {
            String query = "author:" + login + " is:pr is:open";
            return githubClient.searchIssues(bearerToken, GITHUB_API_VERSION, query, 1).totalCount();
        } catch (Exception e) {
            log.warn("Failed to fetch open PR count for {}: {}", login, e.getMessage());
            return 0;
        }
    }

    private List<ActivityDayEntry> fetchActivityHistory(String bearerToken, String login) {
        try {
            LocalDate since = LocalDate.now().minusDays(30);
            List<GithubEventResponse> events = githubClient.getUserEvents(login, bearerToken, GITHUB_API_VERSION, 100);

            return events.stream()
                    .filter(e -> !parseEventDate(e.createdAt()).isBefore(since))
                    .collect(Collectors.groupingBy(
                            e -> parseEventDate(e.createdAt()).toString(),
                            Collectors.counting()
                    ))
                    .entrySet().stream()
                    .map(entry -> new ActivityDayEntry(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(ActivityDayEntry::date))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch activity history for {}: {}", login, e.getMessage());
            return Collections.emptyList();
        }
    }

    private LocalDate parseEventDate(String createdAt) {
        return Instant.parse(createdAt).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String extractFirstLine(String message) {
        if (message == null) return "";
        int newLine = message.indexOf('\n');
        return newLine > 0 ? message.substring(0, newLine) : message;
    }
}
