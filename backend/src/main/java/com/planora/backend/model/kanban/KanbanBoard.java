package com.planora.backend.model.kanban;

import com.planora.backend.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "kanban_boards")
public class KanbanBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kanban_board_id")
    private Long kanbanBoardId;

    private String name;
    private String description;

    private String githubRepository;
    private String githubOwnerName;

    @Column(name = "github_webhook_id")
    private Long githubWebhookId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "kanbanBoard", cascade = CascadeType.ALL)
    private List<KanbanMember> members;

    @OneToMany(mappedBy = "kanbanBoard", cascade = CascadeType.ALL)
    private List<KanbanColumn> columns;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
