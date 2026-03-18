package com.planora.backend.repository;

import com.planora.backend.model.kanban.KanbanMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface kanbanMemberRepository extends JpaRepository<KanbanMember, Long> {
}
