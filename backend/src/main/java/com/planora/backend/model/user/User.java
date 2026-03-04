package com.planora.backend.model.user;

import com.planora.backend.model.user.dto.UserResponse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at",  nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

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

}
