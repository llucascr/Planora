package com.planora.backend.repository;

import com.planora.backend.model.kanban.KanbanColumn;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, Long> {
}
