package com.planora.backend.repository;

import com.planora.backend.model.kanban.KanbanBoard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KanbanBoardRepository extends JpaRepository<KanbanBoard, Long> {

    List<KanbanBoard> findByOwner_UserId(Long userId);
}
