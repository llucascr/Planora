package com.planora.backend.client;

import com.planora.backend.model.issue.dto.GithubWebhookCreateRequest;
import com.planora.backend.model.issue.dto.GithubWebhookResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.PostExchange;

public interface GithubWebhookClient {

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
