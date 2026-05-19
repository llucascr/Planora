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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ApiPythonService {

    private final ApiPythonClient apiPythonClient;
    private final JobRepository jobRepository;
    private final KanbanBoardService kanbanBoardService;
    private final JwtDecoder jwtDecoder;

    public AcceptedResponse generateBacklog(String description, Long boardId, Long columnId, Jwt jwt,
                                            Long userId, String repository) {
        BacklogRequest backlogRequest = saveJobAndgetBacklogRequest(description,  boardId, columnId, jwt, userId, repository);
        return apiPythonClient.generateBacklog(backlogRequest);
    }

    public void saveBacklog(CallbackRequest callbackRequest, Jwt jwt) throws DataNotFoundException {
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
