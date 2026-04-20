package com.planora.backend.model.kanban.dto;

import java.time.LocalDateTime;

public record KanbanMemberResponse(
        Long kanbanMemberId,
        String login,
        String avatarUrl,
        InvitedStatus invitedStatus,
        LocalDateTime invitedAt,
        LocalDateTime joinedAt
) {
}
