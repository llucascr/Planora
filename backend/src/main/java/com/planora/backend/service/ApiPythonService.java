package com.planora.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.planora.backend.client.ApiPythonClient;
import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.Job.Job;
import com.planora.backend.model.Job.JobStatus;
import com.planora.backend.model.Job.dto.CallbackRequest;
import com.planora.backend.model.Job.dto.JobResponse;
import com.planora.backend.model.issue.dto.AcceptedResponse;
import com.planora.backend.model.issue.dto.BacklogRequest;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class ApiPythonService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_DELAY_MS = 3000;

    private final ApiPythonClient apiPythonClient;
    private final JobRepository jobRepository;
    private final KanbanBoardService kanbanBoardService;
    private final ObjectMapper objectMapper;

    public AcceptedResponse generateBacklog(String title, String description, Long boardId, Long columnId, Jwt jwt, Long userId) {
        String repository = kanbanBoardService.findById(boardId).getGithubRepository();
        BacklogRequest backlogRequest = saveJobAndgetBacklogRequest(title, description, boardId, columnId, jwt, userId, repository);
        return generateBacklogWithRetry(backlogRequest);
    }

    private AcceptedResponse generateBacklogWithRetry(BacklogRequest backlogRequest) {
        // TEMP: simula sucesso sem servidor Python
//        return new AcceptedResponse(backlogRequest.jobId(), "accepted");

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

        try {
            kanbanBoardService.createBulkIssuesAndAddToColumn(job.getBoardId(), job.getColumnId(), jwt,
                    issues, job.getUserId(), job.getRepository());
            job.setStatus(JobStatus.COMPLETED);
        } catch (Exception e) {
            job.setStatus(JobStatus.ERROR);
            throw e;
        } finally {
            jobRepository.save(job);
        }
    }

    public JobResponse getJobStatus(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new DataNotFoundException("Job not found"));
        return new JobResponse(job.getId(), job.getStatus(), job.getTitle(), job.getDescription());
    }

    public List<JobResponse> getJobsByBoard(Long boardId) {
        return jobRepository.findByBoardId(boardId).stream()
                .map(job -> new JobResponse(job.getId(), job.getStatus(), job.getTitle(), job.getDescription()))
                .toList();
    }

    private BacklogRequest saveJobAndgetBacklogRequest(String title, String description, Long boardId, Long columnId,
                                                       Jwt jwt, Long userId, String repository) {
        String jsonDescription = toJsonDescription(description);
        Job job = Job.builder()
                .title(title)
                .description(jsonDescription)
                .boardId(boardId)
                .columnId(columnId)
                .jwtToken((String) jwt.getClaims().get("token"))
                .userId(userId)
                .repository(repository)
                .build();

        jobRepository.save(job);
        return new BacklogRequest(job.getId(), job.getDescription());
    }

    private String toJsonDescription(String description) {
        return objectMapper.writeValueAsString(Map.of("description", description));
    }

}
