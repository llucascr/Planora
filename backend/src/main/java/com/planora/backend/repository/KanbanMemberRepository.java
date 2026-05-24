package com.planora.backend.repository;

import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanMember;
import com.planora.backend.model.kanban.dto.InvitedStatus;
import com.planora.backend.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KanbanMemberRepository extends JpaRepository<KanbanMember, Long> {

    Optional<KanbanMember> findByKanbanBoardAndUser(KanbanBoard kanbanBoard, User user);

    Optional<KanbanMember> findByKanbanBoard_KanbanBoardIdAndUser_UserId(Long boardId, Long userId);

    List<KanbanMember> findByKanbanBoard_KanbanBoardId(Long boardId);

    List<KanbanMember> findByUser_UserIdAndInvitedStatus(Long userId, InvitedStatus invitedStatus);
}
