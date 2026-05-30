package com.planora.backend.model.user;

import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.dto.UserIssueResponse;
import com.planora.backend.model.kanban.KanbanMember;
import com.planora.backend.model.user.dto.UserResponse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "tb_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String login;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "profile_url")
    private String profileUrl;

    private String email;

    @Column(name = "notification_email")
    private String notificationEmail;

    @Column(name = "github_token")
    private String githubToken;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at",  nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user")
    private List<Issue> issues;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    @OneToMany(mappedBy = "user")
    private List<KanbanMember> kanbanMemberships;

    public UserResponse toResponse() {
        return new UserResponse(
                this.login,
                this.avatarUrl,
                this.email,
                this.notificationEmail,
                this.createdAt,
                this.updatedAt,
                this.roles
        );
    }

    public UserIssueResponse toIssueResponse() {
        return new UserIssueResponse(
                this.login,
                this.avatarUrl,
                this.email,
                this.notificationEmail
        );
    }

}
