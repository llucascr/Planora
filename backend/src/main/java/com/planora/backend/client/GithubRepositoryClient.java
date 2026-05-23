package com.planora.backend.client;

import com.planora.backend.model.issue.dto.RepositoryResponse;
import com.planora.backend.model.issue.dto.UserRepositoryResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

public interface GithubRepositoryClient {

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
            @RequestHeader(value = "X-GitHub-Api-Version", defaultValue = "2022-11-28") String apiVersion,
            @RequestParam("per_page") int perPage,
            @RequestParam("page") int page
    );
}
