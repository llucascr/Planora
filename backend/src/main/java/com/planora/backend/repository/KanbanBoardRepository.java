package com.planora.backend.repository;

import com.planora.backend.model.kanban.KanbanBoard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KanbanBoardRepository extends JpaRepository<KanbanBoard, Long> {


}
