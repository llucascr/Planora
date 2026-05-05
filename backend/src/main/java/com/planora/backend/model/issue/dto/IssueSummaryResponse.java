package com.planora.backend.model.issue.dto;

import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.State;

public record IssueSummaryResponse(
        Long issueId,
        Integer number,
        String title,
        State state,
        String url
) {

    public static IssueSummaryResponse fromEntity(Issue issue) {
        return new IssueSummaryResponse(
                issue.getIssueId(),
                issue.getNumber(),
                issue.getTitle(),
                issue.getState(),
                issue.getUrl()
        );
    }

}
