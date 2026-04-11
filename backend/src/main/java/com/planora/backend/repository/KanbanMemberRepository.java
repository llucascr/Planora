package com.planora.backend.repository;

import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanMember;
import com.planora.backend.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface KanbanMemberRepository extends JpaRepository<KanbanMember, Long> {

    Optional<KanbanMember> findByKanbanBoardAndUser(KanbanBoard kanbanBoard, User user);

}
