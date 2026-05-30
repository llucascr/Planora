package com.planora.backend.model.issue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.State;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueApiResponse(
        String url,
        Integer number,
        String title,
        String body,
        String state,
        @JsonProperty("user") UserIssueResponse owner,
        List<LabelResponse> labels,
        List<UserIssueResponse> assignees
) {

    public Issue toEntity() {
        Issue issue = new Issue();
        issue.setUrl(this.url);
        issue.setNumber(this.number);
        issue.setTitle(this.title);
        issue.setBody(this.body);
        issue.setState(State.valueOf(this.state.toUpperCase()));
        return issue;
    }

}
