package com.planora.backend.model.kanban;

import com.planora.backend.model.kanban.dto.InvitedStatus;
import com.planora.backend.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "kanban_member", uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "user_id"}))
public class KanbanMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kanban_member_id")
    private Long kanbanMemberId;

    @ManyToOne
    @JoinColumn(name = "board_id")
    private KanbanBoard kanbanBoard;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "invited_status")
    private InvitedStatus invitedStatus;

}
