package com.planora.backend.service;

import com.planora.backend.client.GithubClient;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.Label;
import com.planora.backend.model.issue.dto.IssueApiResponse;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.model.issue.dto.LabelResponse;
import com.planora.backend.model.user.User;
import com.planora.backend.model.user.dto.UserResponse;
import com.planora.backend.repository.IssueRepository;
import com.planora.backend.repository.LabelRepository;
import com.planora.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class GithubService {

    private static final String GITHUB_API_VERSION = "2022-11-28";

    private final IssueRepository issueRepository;
    private final LabelRepository labelRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final GithubClient githubClient;

    public IssueResponse createIssue(String token, IssueRequest issueRequest, Long userId, String repository) {
        User user = userService.findById(userId);

        IssueApiResponse apiResponse = githubClient.createIssue(
                user.getLogin(),
                repository,
                "Bearer " + token,
                GITHUB_API_VERSION,
                issueRequest
        );

        List<Label> labels = apiResponse.labels().stream()
                .map(this::resolveOrCreateLabel)
                .toList();

        List<User> assignees = apiResponse.assignees().stream()
                .map(ur -> resolveAssignee(ur.login()))
                .toList();

        LocalDateTime now = LocalDateTime.now();

        Issue issue = apiResponse.toEntity();
        issue.setUserId(user);
        issue.setLabels(labels);
        issue.setAssignees(assignees);
        issue.setCreatedAt(now);
        issue.setUpdatedAt(now);

        issueRepository.save(issue);

        IssueApiResponse resolvedApiResponse = new IssueApiResponse(
                apiResponse.url(),
                apiResponse.number(),
                apiResponse.title(),
                apiResponse.body(),
                apiResponse.state(),
                user.toResponse(),
                apiResponse.labels(),
                assignees.stream().map(User::toResponse).toList()
        );

        return new IssueResponse(resolvedApiResponse, now, now, null);
    }

    private Label resolveOrCreateLabel(LabelResponse labelResponse) {
        return labelRepository.findByName(labelResponse.name())
                .orElseGet(() -> {
                    Label label = new Label();
                    label.setUrl(labelResponse.url());
                    label.setName(labelResponse.name());
                    label.setColor(labelResponse.color());
                    label.setDescription(labelResponse.description());
                    return labelRepository.save(label);
                });
    }

    private User resolveAssignee(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + login));
    }

}
