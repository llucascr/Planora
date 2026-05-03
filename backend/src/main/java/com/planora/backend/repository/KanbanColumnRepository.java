package com.planora.backend.repository;

import com.planora.backend.model.kanban.KanbanColumn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, Long> {
    List<KanbanColumn> findByKanbanBoard_KanbanBoardIdOrderByPosition(Long boardId);
    List<KanbanColumn> findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(Long boardId);
}
