package com.planora.backend.repository;

import com.planora.backend.model.issue.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    @Query("SELECT i FROM Issue i WHERE i.number = :number AND i.column.kanbanBoard.kanbanBoardId = :boardId")
    Optional<Issue> findByNumberAndBoardId(@Param("number") Integer number, @Param("boardId") Long boardId);

    @Query("SELECT COUNT(i) FROM Issue i JOIN i.assignees a WHERE a.userId = :userId AND i.updatedAt >= :since")
    long countAssignedIssuesSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT i FROM Issue i JOIN i.assignees a WHERE a.userId = :userId AND i.createdAt >= :since")
    List<Issue> findAssignedIssuesCreatedSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT i FROM Issue i JOIN i.assignees a WHERE a.userId = :userId AND i.closedAt >= :since AND i.state = com.planora.backend.model.issue.State.CLOSED")
    List<Issue> findAssignedIssuesClosedSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
