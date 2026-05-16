package com.planora.backend.service;

import com.planora.backend.client.ApiPythonClient;
import com.planora.backend.exception.DataAlreadyExistException;
import com.planora.backend.model.Job.Job;
import com.planora.backend.model.issue.dto.AcceptedResponse;
import com.planora.backend.model.issue.dto.BacklogRequest;
import com.planora.backend.repository.ApiPythonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ApiPythonService {

    private final ApiPythonRepository apiPythonRepository;
    private  ApiPythonClient apiPythonClient;

    public AcceptedResponse generateBacklog(String description) {
        BacklogRequest backlogRequest = saveJobAndgetBacklogRequest(description);

        if (apiPythonRepository.findById(backlogRequest.jobId()).isPresent()) {
            throw new DataAlreadyExistException("The Job is already being processed or has already been processed.");
        }

        return new AcceptedResponse(
                backlogRequest.jobId(),
                backlogRequest.description()
        );
    }

    private BacklogRequest saveJobAndgetBacklogRequest(String description) {
        Job job = Job.builder()
                .description(description)
                .build();

        apiPythonRepository.save(job);
        return new BacklogRequest(job.getId(), job.getDescription());
    }

}
