package com.planora.backend.client;

import com.planora.backend.model.issue.dto.LabelResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

public interface GithubLabelClient {

    @GetExchange("/repos/{owner}/{repo}/labels")
    List<LabelResponse> getRepositoryLabels(
            @PathVariable("owner") String owner,
            @PathVariable("repo") String repo,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-GitHub-Api-Version") String apiVersion
    );
}
