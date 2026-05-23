package com.planora.backend.service;

import com.planora.backend.client.ApiPythonClient;
import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.Job.Job;
import com.planora.backend.model.Job.dto.CallbackRequest;
import com.planora.backend.model.issue.dto.AcceptedResponse;
import com.planora.backend.model.issue.dto.BacklogRequest;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ApiPythonService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_DELAY_MS = 3000;

    private final ApiPythonClient apiPythonClient;
    private final JobRepository jobRepository;
    private final KanbanBoardService kanbanBoardService;
    private final JwtDecoder jwtDecoder;

    public AcceptedResponse generateBacklog(String description, Long boardId, Long columnId, Jwt jwt,
                                            Long userId, String repository) {
        BacklogRequest backlogRequest = saveJobAndgetBacklogRequest(description, boardId, columnId, jwt, userId, repository);
        return generateBacklogWithRetry(backlogRequest);
    }

    private AcceptedResponse generateBacklogWithRetry(BacklogRequest backlogRequest) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return apiPythonClient.generateBacklog(backlogRequest);
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} to reach Python AI server failed: {}", attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting to retry Python AI request", ie);
                    }
                }
            }
        }
        throw new RuntimeException("Python AI server unavailable after " + MAX_ATTEMPTS + " attempts", lastException);
    }

    public void saveBacklog(CallbackRequest callbackRequest, Jwt jwt) throws DataNotFoundException {
        if (callbackRequest.backlog() == null || callbackRequest.backlog().isEmpty()) {
            log.warn("Callback for job={} received with empty or null backlog, skipping", callbackRequest.jobId());
            return;
        }

        Job job = jobRepository.findById(callbackRequest.jobId())
                .orElseThrow(() -> new DataNotFoundException("Job not found"));

        List<IssueRequest> issues = callbackRequest.backlog().stream()
                .map(issue -> new IssueRequest(issue.title(), issue.body(), List.of(), List.of()))
                .toList();

        kanbanBoardService.createBulkIssuesAndAddToColumn(job.getBoardId(), job.getColumnId(), jwt,
                issues, job.getUserId(), job.getRepository());
    }

    private BacklogRequest saveJobAndgetBacklogRequest(String description, Long boardId, Long columnId, Jwt jwt,
                                                       Long userId, String repository) {
        Job job = Job.builder()
                .description(description)
                .boardId(boardId)
                .columnId(columnId)
                .jwtToken((String) jwt.getClaims().get("token"))
                .userId(userId)
                .repository(repository)
                .build();

        jobRepository.save(job);
        return new BacklogRequest(job.getId(), job.getDescription());
    }

}
