package com.planora.backend.repository;

import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.dto.InvitedStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KanbanBoardRepository extends JpaRepository<KanbanBoard, Long> {

    List<KanbanBoard> findByMembers_User_UserIdAndMembers_InvitedStatus(Long userId, InvitedStatus status);
}
