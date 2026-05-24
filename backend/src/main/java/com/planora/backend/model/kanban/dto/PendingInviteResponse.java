package com.planora.backend.model.kanban.dto;

import java.time.LocalDateTime;

public record PendingInviteResponse(
        Long kanbanMemberId,
        Long boardId,
        String boardName,
        String boardDescription,
        String invitedByLogin,
        LocalDateTime invitedAt
) {
}
