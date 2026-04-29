package com.planora.backend.repository;

import com.planora.backend.model.issue.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    Optional<Issue> findByNumberAndColumn_KanbanBoard_KanbanBoardId(Integer number, Long boardId);
}
