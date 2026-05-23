package com.planora.backend.controller;

import com.planora.backend.model.issue.Label;
import com.planora.backend.model.issue.dto.LabelResponse;
import com.planora.backend.model.issue.dto.UserRepositoryResponse;
import com.planora.backend.service.GithubService;
import com.planora.backend.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/github")
public class GithubController {

    private final GithubService githubService;
    private final TokenService tokenService;

    @GetMapping("/repositories")
    public ResponseEntity<List<UserRepositoryResponse>> getUserRepositories(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(githubService.listUserRepositories(tokenService.getGithubToken(jwt)));
    }

    @GetMapping("/repository/labels")
    public ResponseEntity<List<LabelResponse>> getRepositoryLabels(
            @RequestParam String ownerName,
            @RequestParam String repository,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                githubService.listRepositoryLabels(
                        jwt,
                        ownerName,
                        repository
                )
        );
    }
}
