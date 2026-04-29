package com.planora.backend.client;

import com.planora.backend.model.issue.dto.GithubWebhookCreateRequest;
import com.planora.backend.model.issue.dto.GithubWebhookResponse;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueApiResponse;
import com.planora.backend.model.issue.dto.RepositoryResponse;
import com.planora.backend.model.issue.dto.UserRepositoryResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

public interface GithubClient {

    @PostExchange("/repos/{user}/{repo}/issues")
    IssueApiResponse createIssue(
            @PathVariable String user,
            @PathVariable String repo,
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-GitHub-Api-Version", defaultValue = "2022-11-28") String apiVersion,
            @RequestBody IssueRequest body
    );

    @GetExchange("repos/{user}/{repo}")
    RepositoryResponse getRepository(
            @PathVariable String user,
            @PathVariable String repo,
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-GitHub-Api-Version", defaultValue = "2022-11-28") String apiVersion
    );

    @GetExchange("/user/repos")
    List<UserRepositoryResponse> getUserRepositories(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-GitHub-Api-Version", defaultValue = "2022-11-28") String apiVersion
    );

    @PostExchange("/repos/{owner}/{repo}/hooks")
    GithubWebhookResponse createWebhook(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-GitHub-Api-Version", defaultValue = "2022-11-28") String apiVersion,
            @RequestBody GithubWebhookCreateRequest body
    );

    @DeleteExchange("/repos/{owner}/{repo}/hooks/{hookId}")
    void deleteWebhook(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable Long hookId,
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-GitHub-Api-Version", defaultValue = "2022-11-28") String apiVersion
    );

}
