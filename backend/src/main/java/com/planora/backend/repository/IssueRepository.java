package com.planora.backend.repository;

import com.planora.backend.model.issue.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    @Query("SELECT i FROM Issue i WHERE i.number = :number AND i.column.kanbanBoard.kanbanBoardId = :boardId")
    Optional<Issue> findByNumberAndBoardId(@Param("number") Integer number, @Param("boardId") Long boardId);
}
