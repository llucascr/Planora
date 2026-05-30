package com.planora.backend.model.Job.dto;

import com.planora.backend.model.Job.JobStatus;

public record JobResponse(Long id, JobStatus status, String title, String description) {
}
