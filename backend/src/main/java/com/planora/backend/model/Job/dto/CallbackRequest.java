package com.planora.backend.model.Job.dto;

import com.planora.backend.model.issue.dto.BacklogRequest;
import com.planora.backend.model.issue.dto.IssueRequest;

import java.util.List;

public record CallbackRequest(List<IssueRequest> backlog, Long jobId) {
}
