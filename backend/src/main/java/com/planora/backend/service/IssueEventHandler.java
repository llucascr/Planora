package com.planora.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.Label;
import com.planora.backend.model.issue.State;
import com.planora.backend.model.issue.dto.IssueWebhookPayload;
import com.planora.backend.model.issue.dto.LabelResponse;
import com.planora.backend.model.issue.dto.UserIssueResponse;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.IssueRepository;
import com.planora.backend.repository.KanbanBoardRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class IssueEventHandler implements WebhookEventHandler {

    private static final Logger log = LoggerFactory.getLogger(IssueEventHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final KanbanBoardRepository kanbanBoardRepository;
    private final IssueRepository issueRepository;
    private final LabelService labelService;
    private final UserService userService;

    @Override
    public boolean supports(String eventType) {
        return "issues".equals(eventType);
    }

    @Override
    @Transactional
    public void handle(String rawPayload) {
        IssueWebhookPayload payload = parsePayload(rawPayload);
        if (payload == null || payload.issue() == null || payload.repository() == null) {
            return;
        }

        String owner = payload.repository().owner().login();
        String repo = payload.repository().name();

        log.info("Webhook '{}' received for {}/{} issue #{}", payload.action(), owner, repo, payload.issue().number());

        List<KanbanBoard> boards = kanbanBoardRepository.findByGithubOwnerAndRepositoryIgnoreCase(owner, repo);
        if (boards.isEmpty()) {
            log.warn("No boards found for repository {}/{} — skipping", owner, repo);
            return;
        }

        log.info("Syncing issue #{} to {} board(s)", payload.issue().number(), boards.size());
        boards.forEach(board -> syncIssueToBoard(payload.issue(), board));
    }

    private void syncIssueToBoard(IssueWebhookPayload.WebhookIssueData issueData, KanbanBoard board) {
        Optional<Issue> existing = issueRepository.findByNumberAndBoardId(
                issueData.number(), board.getKanbanBoardId()
        );

        if (existing.isPresent()) {
            updateExistingIssue(existing.get(), issueData);
        } else {
            createIssueInFirstColumn(issueData, board);
        }
    }

    private void updateExistingIssue(Issue issue, IssueWebhookPayload.WebhookIssueData issueData) {
        issue.setTitle(issueData.title());
        issue.setBody(issueData.body());
        issue.setState(State.valueOf(issueData.state().toUpperCase()));
        issue.setLabels(resolveLabels(issueData.labels()));
        issue.setAssignees(resolveAssignees(issueData.assignees()));
        issue.setUpdatedAt(LocalDateTime.now());
        if (issueData.closedAt() != null) {
            issue.setClosedAt(parseDateTime(issueData.closedAt()));
        }
        issueRepository.save(issue);
    }

    private void createIssueInFirstColumn(IssueWebhookPayload.WebhookIssueData issueData, KanbanBoard board) {
        KanbanColumn firstColumn = board.getColumns().stream()
                .min((a, b) -> Integer.compare(a.getPosition(), b.getPosition()))
                .orElse(null);

        if (firstColumn == null) {
            log.warn("Board {} has no columns, cannot add issue #{}", board.getKanbanBoardId(), issueData.number());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Issue issue = new Issue();
        issue.setUrl(issueData.url());
        issue.setNumber(issueData.number());
        issue.setTitle(issueData.title());
        issue.setBody(issueData.body());
        issue.setState(State.valueOf(issueData.state().toUpperCase()));
        issue.setLabels(resolveLabels(issueData.labels()));
        issue.setAssignees(new ArrayList<>());
        issue.setColumn(firstColumn);
        issue.setCreatedAt(issueData.createdAt() != null ? parseDateTime(issueData.createdAt()) : now);
        issue.setUpdatedAt(now);
        issueRepository.save(issue);
    }

    private List<User> resolveAssignees(List<UserIssueResponse> assignees) {
        if (assignees == null) return new ArrayList<>();
        return assignees.stream()
                .map(a -> userService.findOptionalByLogin(a.login()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Label> resolveLabels(List<LabelResponse> labelResponses) {
        if (labelResponses == null) return new ArrayList<>();
        return labelResponses.stream()
                .map(labelService::resolveOrCreateLabel)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private LocalDateTime parseDateTime(String iso8601) {
        try {
            return OffsetDateTime.parse(iso8601).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private IssueWebhookPayload parsePayload(String rawPayload) {
        try {
            return MAPPER.readValue(rawPayload, IssueWebhookPayload.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse webhook payload: {}", e.getMessage());
            return null;
        }
    }
}
