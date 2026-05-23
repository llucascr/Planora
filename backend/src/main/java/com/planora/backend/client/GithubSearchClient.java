package com.planora.backend.client;

import com.planora.backend.model.dashboard.dto.GithubCommitSearchResponse;
import com.planora.backend.model.dashboard.dto.GithubEventResponse;
import com.planora.backend.model.dashboard.dto.GithubSearchResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

public interface GithubSearchClient {

    @GetExchange("/search/commits")
    GithubCommitSearchResponse searchCommits(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-GitHub-Api-Version", defaultValue = "2022-11-28") String apiVersion,
            @RequestHeader("Accept") String accept,
            @RequestParam("q") String query,
            @RequestParam("per_page") int perPage,
            @RequestParam("sort") String sort,
            @RequestParam("order") String order
    );

    @GetExchange("/search/issues")
    GithubSearchResponse searchIssues(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-GitHub-Api-Version", defaultValue = "2022-11-28") String apiVersion,
            @RequestParam("q") String query,
            @RequestParam("per_page") int perPage
    );

    @GetExchange("/users/{username}/events")
    List<GithubEventResponse> getUserEvents(
            @PathVariable String username,
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-GitHub-Api-Version", defaultValue = "2022-11-28") String apiVersion,
            @RequestParam("per_page") int perPage
    );
}
