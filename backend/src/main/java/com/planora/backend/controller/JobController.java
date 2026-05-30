package com.planora.backend.controller;

import com.planora.backend.model.Job.dto.JobResponse;
import com.planora.backend.service.ApiPythonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/jobs")
public class JobController {

    private final ApiPythonService apiPythonService;

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobStatus(@PathVariable Long id) {
        return ResponseEntity.ok(apiPythonService.getJobStatus(id));
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> getJobsByBoard(@RequestParam Long boardId) {
        return ResponseEntity.ok(apiPythonService.getJobsByBoard(boardId));
    }

}
