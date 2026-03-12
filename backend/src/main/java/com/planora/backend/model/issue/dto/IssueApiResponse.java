package com.planora.backend.model.issue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.Label;
import com.planora.backend.model.issue.State;
import com.planora.backend.model.user.User;
import com.planora.backend.model.user.dto.UserResponse;

import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueApiResponse(
        String url,
        Integer number,
        String title,
        String body,
        String state,
        @JsonProperty("user") UserResponse owner,
        List<LabelResponse> labels,
        List<UserResponse> assignees
) {

    public Issue toEntity() {
        Issue issue = new Issue();
        issue.setUrl(this.url);
        issue.setNumber(this.number);
        issue.setTitle(this.title);
        issue.setBody(this.body);
        issue.setState(State.valueOf(this.state.toUpperCase()));
        issue.setLabels(this.labels.stream().map(IssueApiResponse::toLabelEntity).toList());
        issue.setAssignees(this.assignees.stream().map(IssueApiResponse::toUserEntity).toList());
        return issue;
    }

    private static User toUserEntity(UserResponse userResponse) {
        return User.builder()
                .login(userResponse.login())
                .avatarUrl(userResponse.avatarUrl())
                .email(userResponse.email())
                .notificationEmail(userResponse.notificationEmail())
                .createdAt(userResponse.createdAt())
                .updatedAt(userResponse.updatedAt())
                .roles(userResponse.roles())
                .build();
    }

    private static Label toLabelEntity(LabelResponse labelResponse) {
        Label label = new Label();
        label.setUrl(labelResponse.url());
        label.setName(labelResponse.name());
        label.setColor(labelResponse.color());
        label.setDescription(labelResponse.description());
        return label;
    }

}
