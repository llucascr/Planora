package com.planora.backend.service;

import com.planora.backend.client.GithubClient;
import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.Label;
import com.planora.backend.model.issue.State;
import com.planora.backend.model.issue.dto.IssueApiResponse;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.model.issue.dto.IssueUpdateRequest;
import com.planora.backend.model.issue.dto.UserRepositoryResponse;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
                new IssueUpdateRequest("open")
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
                new IssueUpdateRequest("closed")
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
                    new IssueUpdateRequest("closed")
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
        return githubClient.getUserRepositories("Bearer " + githubToken, GITHUB_API_VERSION);
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

}
