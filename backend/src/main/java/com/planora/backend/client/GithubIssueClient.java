package com.planora.backend.client;

import com.planora.backend.model.issue.dto.IssueApiResponse;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueUpdateRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

public interface GithubIssueClient {

    @PostExchange("/repos/{user}/{repo}/issues")
    IssueApiResponse createIssue(
            @PathVariable String user,
            @PathVariable String repo,
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-GitHub-Api-Version", defaultValue = "2022-11-28") String apiVersion,
            @RequestBody IssueRequest body
    );

    @PatchExchange("/repos/{user}/{repo}/issues/{issueNumber}")
    IssueApiResponse updateIssue(
            @PathVariable String user,
            @PathVariable String repo,
            @PathVariable Integer issueNumber,
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-GitHub-Api-Version", defaultValue = "2022-11-28") String apiVersion,
            @RequestBody IssueUpdateRequest body
    );
}
