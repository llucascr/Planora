package com.planora.backend.model.issue.dto;

import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.State;
import com.planora.backend.model.user.User;

import java.util.List;

public record IssueSummaryResponse(
        Long issueId,
        Integer number,
        String title,
        State state,
        String body,
        String url,
        List<IssueAssigneeResponse> assignees
) {

    public static IssueSummaryResponse fromEntity(Issue issue) {
        return new IssueSummaryResponse(
                issue.getIssueId(),
                issue.getNumber(),
                issue.getTitle(),
                issue.getState(),
                issue.getBody(),
                issue.getUrl(),
                issue.getAssignees()
                        .stream()
                        .map(user -> new IssueAssigneeResponse(
                                user.getLogin(),
                                user.getAvatarUrl()
                        ))
                        .toList()
        );
    }

}
