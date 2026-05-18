package com.planora.backend.service;

import com.planora.backend.client.GithubClient;
import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.Label;
import com.planora.backend.model.issue.dto.*;
import com.planora.backend.model.issue.State;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class GithubService {

    private static final Logger log = LoggerFactory.getLogger(GithubService.class);
    private static final String GITHUB_API_VERSION = "2022-11-28";

    private final IssueRepository issueRepository;
    private final UserService userService;
    private final LabelService labelService;
    private final GithubClient githubClient;
    private final TokenService tokenService;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${github.webhook.secret:}")
    private String webhookSecret;

    public IssueResponse createIssue(Jwt token, IssueRequest issueRequest, Long userId, String repository, KanbanColumn column) {
        User user = userService.findById(userId);
        return buildAndPersistIssue(user, repository, token, column, issueRequest);
    }

    @Transactional
    public List<IssueResponse> createBulkIssues(Jwt token, List<IssueRequest> requests, Long userId, String repository, KanbanColumn column) {
        User user = userService.findById(userId);
        return requests.stream()
                .map(request -> buildAndPersistIssue(user, repository, token, column, request))
                .toList();
    }

    @Transactional
    public IssueResponse openIssue(Jwt token, Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new DataNotFoundException("Issue not found"));

        KanbanBoard board = issue.getColumn().getKanbanBoard();

        IssueApiResponse apiResponse = githubClient.updateIssue(
                board.getGithubOwnerName(),
                board.getGithubRepository(),
                issue.getNumber(),
                "Bearer " + tokenService.getGithubToken(token),
                GITHUB_API_VERSION,
                new IssueUpdateRequest(null, null, "open", null, null)
        );

        issue.setState(State.OPEN);
        issue.setClosedAt(null);
        issue.setUpdatedAt(LocalDateTime.now());
        issueRepository.save(issue);

        IssueApiResponse resolvedApiResponse = setUpIssueApiResponse(apiResponse, issue.getUser());

        return new IssueResponse(resolvedApiResponse, issue.getCreatedAt(), issue.getUpdatedAt(), null);
    }

    @Transactional
    public IssueResponse closeIssue(Jwt token, Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new DataNotFoundException("Issue not found"));

        KanbanBoard board = issue.getColumn().getKanbanBoard();

        IssueApiResponse apiResponse = githubClient.updateIssue(
                board.getGithubOwnerName(),
                board.getGithubRepository(),
                issue.getNumber(),
                "Bearer " + tokenService.getGithubToken(token),
                GITHUB_API_VERSION,
                new IssueUpdateRequest(null, null, "closed", null, null)
        );

        LocalDateTime now = LocalDateTime.now();
        issue.setState(State.CLOSED);
        issue.setClosedAt(now);
        issue.setUpdatedAt(now);
        issueRepository.save(issue);

        IssueApiResponse resolvedApiResponse = setUpIssueApiResponse(apiResponse, issue.getUser());

        return new IssueResponse(resolvedApiResponse, issue.getCreatedAt(), issue.getUpdatedAt(), issue.getClosedAt());
    }

    @Transactional
    public IssueResponse updateIssue(Jwt token, Long issueId, IssueUpdateRequest request) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new DataNotFoundException("Issue not found"));

        KanbanBoard board = issue.getColumn().getKanbanBoard();

        IssueApiResponse apiResponse = githubClient.updateIssue(
                board.getGithubOwnerName(),
                board.getGithubRepository(),
                issue.getNumber(),
                "Bearer " + tokenService.getGithubToken(token),
                GITHUB_API_VERSION,
                request
        );

        syncIssueFromApiResponse(issue, apiResponse);
        issueRepository.save(issue);

        IssueApiResponse resolvedApiResponse = setUpIssueApiResponse(apiResponse, issue.getUser());
        return new IssueResponse(resolvedApiResponse, issue.getCreatedAt(), issue.getUpdatedAt(), issue.getClosedAt());
    }

    @Transactional
    public void deleteIssue(Jwt token, Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new DataNotFoundException("Issue not found"));

        if (issue.getState() != State.CLOSED) {
            KanbanBoard board = issue.getColumn().getKanbanBoard();
            githubClient.updateIssue(
                    board.getGithubOwnerName(),
                    board.getGithubRepository(),
                    issue.getNumber(),
                    "Bearer " + tokenService.getGithubToken(token),
                    GITHUB_API_VERSION,
                    new IssueUpdateRequest(null, null, "closed", null, null) // TODO: Melhorar essa logica de deleção
            );
        }

        issueRepository.delete(issue);
    }

    private IssueResponse buildAndPersistIssue(User user, String repository, Jwt token, KanbanColumn column, IssueRequest issueRequest) {
        IssueApiResponse apiResponse = githubClient.createIssue(
                user.getLogin(),
                repository,
                "Bearer " + tokenService.getGithubToken(token),
                GITHUB_API_VERSION,
                issueRequest
        );

        List<Label> labels = getLabelsAddThemApiResponse(apiResponse);
        List<User> assignees = getUsersAddThemApiResponse(apiResponse);

        Issue issue = setUpIssue(apiResponse, user, labels, assignees);
        issue.setColumn(column);
        issueRepository.save(issue);

        IssueApiResponse resolvedApiResponse = setUpIssueApiResponse(apiResponse, user);

        return new IssueResponse(resolvedApiResponse, issue.getCreatedAt(), issue.getUpdatedAt(), null);
    }

    public List<UserRepositoryResponse> listUserRepositories(String githubToken) {
        List<UserRepositoryResponse> all = new ArrayList<>();
        int page = 1;
        final int perPage = 100;
        List<UserRepositoryResponse> pageResult;
        do {
            pageResult = githubClient.getUserRepositories("Bearer " + githubToken, GITHUB_API_VERSION, perPage, page++);
            all.addAll(pageResult);
        } while (pageResult.size() == perPage);
        return all;
    }

    public boolean checkIfRepositoryAndOwnerNameAreValid(String token, String ownerName, String repository) {
        try {
            return githubClient
                    .getRepository(ownerName, repository, "Bearer " + token, GITHUB_API_VERSION) != null;
        } catch (Exception e) {
            log.warn("Repository validation failed for owner='{}' repo='{}': {}", ownerName, repository, e.getMessage());
            return false;
        }
    }

    public Long createRepositoryWebhook(String githubToken, String owner, String repo) {
        if (isLocalUrl(appBaseUrl)) {
            log.warn("Skipping webhook creation for {}/{}: APP_BASE_URL='{}' is not publicly reachable. " +
                     "Set APP_BASE_URL to a public URL (e.g. via ngrok) to enable webhook integration.",
                     owner, repo, appBaseUrl);
            return null;
        }
        GithubWebhookCreateRequest request = new GithubWebhookCreateRequest(
                "web",
                new GithubWebhookCreateRequest.GithubWebhookConfig(
                        buildWebhookUrl(),
                        "json",
                        webhookSecret.isBlank() ? null : webhookSecret,
                        "0"
                ),
                List.of("issues"),
                true
        );
        GithubWebhookResponse response = githubClient.createWebhook(
                owner, repo, "Bearer " + githubToken, GITHUB_API_VERSION, request
        );
        log.info("GitHub webhook created (id={}) for {}/{}", response.id(), owner, repo);
        return response.id();
    }

    private String buildWebhookUrl() {
        try {
            URI uri = URI.create(appBaseUrl);
            String origin = uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
            return origin + "/v1/webhook/github/issues";
        } catch (Exception e) {
            return appBaseUrl.replaceAll("/+$", "") + "/v1/webhook/github/issues";
        }
    }

    private boolean isLocalUrl(String url) {
        if (url == null) return true;
        String lower = url.toLowerCase();
        return lower.contains("localhost") || lower.contains("127.0.0.1") || lower.contains("0.0.0.0");
    }

    public void deleteRepositoryWebhook(String githubToken, String owner, String repo, Long webhookId) {
        try {
            githubClient.deleteWebhook(owner, repo, webhookId, "Bearer " + githubToken, GITHUB_API_VERSION);
            log.info("GitHub webhook deleted (id={}) for {}/{}", webhookId, owner, repo);
        } catch (Exception e) {
            log.warn("Failed to delete webhook {} for {}/{}: {}", webhookId, owner, repo, e.getMessage());
        }
    }

    private static @NonNull IssueApiResponse setUpIssueApiResponse(IssueApiResponse apiResponse, User user) {
        return new IssueApiResponse(
                apiResponse.url(),
                apiResponse.number(),
                apiResponse.title(),
                apiResponse.body(),
                apiResponse.state(),
                user.toIssueResponse(),
                apiResponse.labels(),
                apiResponse.assignees()
        );
    }

    private static @NonNull Issue setUpIssue(IssueApiResponse apiResponse, User user, List<Label> labels, List<User> assignees) {
        LocalDateTime now = LocalDateTime.now();
        Issue issue = apiResponse.toEntity();

        issue.setUser(user);
        issue.setLabels(labels);
        issue.setAssignees(assignees);
        issue.setCreatedAt(now);
        issue.setUpdatedAt(now);

        return issue;
    }

    private void syncIssueFromApiResponse(Issue issue, IssueApiResponse apiResponse) {
        LocalDateTime now = LocalDateTime.now();
        issue.setTitle(apiResponse.title());
        issue.setBody(apiResponse.body());

        issue.getLabels().clear();
        issue.getLabels().addAll(getLabelsAddThemApiResponse(apiResponse));

        issue.getAssignees().clear();
        issue.getAssignees().addAll(getUsersAddThemApiResponse(apiResponse));

        State newState = State.valueOf(apiResponse.state().toUpperCase());
        issue.setState(newState);
        issue.setClosedAt(newState == State.CLOSED ? now : null);
        issue.setUpdatedAt(now);
    }

    private @NonNull List<User> getUsersAddThemApiResponse(IssueApiResponse apiResponse) {
        return apiResponse.assignees().stream()
                .map(ur -> userService.findOptionalByLogin(ur.login()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private @NonNull List<Label> getLabelsAddThemApiResponse(IssueApiResponse apiResponse) {
        return apiResponse.labels().stream()
                .map(labelService::resolveOrCreateLabel)
                .toList();
    }

    public List<LabelResponse> listRepositoryLabels(
            Jwt token,
            String ownerName,
            String repository
    ) {
        return githubClient.getRepositoryLabels(
                        ownerName,
                        repository,
                        "Bearer " + tokenService.getGithubToken(token),
                        GITHUB_API_VERSION
                ).stream()
                .map(labelService::resolveOrCreateLabel)
                .map(LabelResponse::fromEntity)
                .toList();
    }
}
