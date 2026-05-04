package com.planora.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.Label;
import com.planora.backend.model.issue.State;
import com.planora.backend.model.user.User;
import com.planora.backend.model.issue.dto.IssueWebhookPayload;
import com.planora.backend.model.issue.dto.LabelResponse;
import com.planora.backend.model.issue.dto.UserIssueResponse;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.repository.IssueRepository;
import com.planora.backend.repository.KanbanBoardRepository;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final KanbanBoardRepository kanbanBoardRepository;
    private final IssueRepository issueRepository;
    private final LabelService labelService;
    private final UserService userService;

    @Value("${github.webhook.secret:}")
    private String webhookSecret;

    @Transactional
    public void processIssueEvent(String event, String signature, String rawPayload) {
        if (!"issues".equals(event)) {
            return;
        }

        if (!isValidSignature(rawPayload, signature)) {
            log.warn("Received webhook with invalid signature — ignoring");
            return;
        }

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

    private boolean isValidSignature(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("GITHUB_WEBHOOK_SECRET not configured — skipping signature verification");
            return true;
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        String receivedHmac = signatureHeader.substring(7);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedHmac = HexFormat.of().formatHex(hmacBytes);
            return MessageDigest.isEqual(
                    receivedHmac.getBytes(StandardCharsets.UTF_8),
                    computedHmac.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
