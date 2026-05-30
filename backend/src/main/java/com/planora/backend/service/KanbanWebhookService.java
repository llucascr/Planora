package com.planora.backend.service;

import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.repository.KanbanBoardRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class KanbanWebhookService {

    private static final Logger log = LoggerFactory.getLogger(KanbanWebhookService.class);

    private final KanbanBoardRepository kanbanBoardRepository;
    private final GithubService githubService;

    public KanbanBoard registerWebhook(Long boardId, String githubToken) {
        KanbanBoard board = kanbanBoardRepository.findById(boardId)
                .orElseThrow(() -> new DataNotFoundException("Kanban Board with id " + boardId + " not found"));

        if (board.getGithubWebhookId() != null) {
            return board;
        }

        String owner = board.getGithubOwnerName();
        String repo = board.getGithubRepository();

        Long webhookId = kanbanBoardRepository
                .findBoardsByOwnerAndRepositoryWithWebhook(owner, repo)
                .stream().findFirst()
                .map(KanbanBoard::getGithubWebhookId)
                .orElseGet(() -> githubService.createRepositoryWebhook(githubToken, owner, repo));

        board.setGithubWebhookId(webhookId);
        kanbanBoardRepository.save(board);
        return board;
    }

    public void registerWebhookIfNeeded(KanbanBoard board, String githubToken, String owner, String repo) {
        Long webhookId = kanbanBoardRepository
                .findBoardsByOwnerAndRepositoryWithWebhook(owner, repo)
                .stream().findFirst()
                .map(KanbanBoard::getGithubWebhookId)
                .orElseGet(() -> {
                    try {
                        return githubService.createRepositoryWebhook(githubToken, owner, repo);
                    } catch (Exception e) {
                        log.warn("Could not create GitHub webhook for {}/{}: {}", owner, repo, e.getMessage());
                        return null;
                    }
                });
        board.setGithubWebhookId(webhookId);
        kanbanBoardRepository.save(board);
    }

    public void removeWebhookIfLastBoard(KanbanBoard board) {
        if (board.getGithubWebhookId() == null) return;

        boolean hasOtherBoards = !kanbanBoardRepository
                .findByOwnerAndRepositoryExcluding(
                        board.getGithubOwnerName(),
                        board.getGithubRepository(),
                        board.getKanbanBoardId()
                ).isEmpty();

        if (!hasOtherBoards) {
            githubService.deleteRepositoryWebhook(
                    board.getOwner().getGithubToken(),
                    board.getGithubOwnerName(),
                    board.getGithubRepository(),
                    board.getGithubWebhookId()
            );
        }
    }
}
