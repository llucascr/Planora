package com.planora.backend.repository;

import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.dto.InvitedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KanbanBoardRepository extends JpaRepository<KanbanBoard, Long> {

    @Query("SELECT kb FROM KanbanBoard kb JOIN kb.members m WHERE m.user.userId = :userId AND m.invitedStatus = :status")
    List<KanbanBoard> findBoardsByMemberUserIdAndStatus(@Param("userId") Long userId, @Param("status") InvitedStatus status);

    @Query("SELECT kb FROM KanbanBoard kb WHERE LOWER(kb.githubOwnerName) = LOWER(:ownerName) AND LOWER(kb.githubRepository) = LOWER(:repository)")
    List<KanbanBoard> findByGithubOwnerAndRepositoryIgnoreCase(@Param("ownerName") String ownerName, @Param("repository") String repository);

    @Query("SELECT kb FROM KanbanBoard kb WHERE kb.githubOwnerName = :ownerName AND kb.githubRepository = :repository AND kb.githubWebhookId IS NOT NULL")
    List<KanbanBoard> findBoardsByOwnerAndRepositoryWithWebhook(@Param("ownerName") String ownerName, @Param("repository") String repository);

    @Query("SELECT kb FROM KanbanBoard kb WHERE kb.githubOwnerName = :ownerName AND kb.githubRepository = :repository AND kb.kanbanBoardId <> :excludeBoardId")
    List<KanbanBoard> findByOwnerAndRepositoryExcluding(@Param("ownerName") String ownerName, @Param("repository") String repository, @Param("excludeBoardId") Long excludeBoardId);
}
