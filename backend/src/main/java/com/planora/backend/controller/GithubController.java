package com.planora.backend.controller;

import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.service.GithubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/github")
public class GithubController {

    private final GithubService githubService;

    @PostMapping
    public ResponseEntity<IssueResponse> createIssue(
            @RequestHeader("token") String token,
            @RequestBody IssueRequest issueRequest,
            @RequestParam Long userId,
            @RequestParam String repository) {
        return ResponseEntity.ok(githubService.createIssue(token, issueRequest, userId, repository));
    }


}
