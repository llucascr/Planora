package com.planora.backend.service;

import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.kanban.dto.KanbanBoardRequest;
import com.planora.backend.repository.KanbanBoardRepository;
import com.planora.backend.repository.KanbanColumnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class KanbanBoardService {

    private final KanbanBoardRepository kanbanBoardRepository;

    private final KanbanColumnRepository kanbanColumnRepository;

    private final UserService userService;

    private final GithubService githubService;

    public void createKanbanBoard(String token, KanbanBoardRequest request, Long userId) {
//        if (!githubService.checkIfRepositoryAndOwnerNameAreValid(token, request.githubOwnerName(), request.githubRepository())) {
//            throw new DataNotFoundException("Repository " + request.githubRepository() + " not found for owner " + request.githubOwnerName());
//        }

        KanbanBoard kanbanBoard = KanbanBoard.builder()
                .name(request.name())
                .description(request.description())
                .owner(userService.findById(userId))
                .githubRepository(request.githubRepository())
                .githubOwnerName(request.githubOwnerName())
                .members(new ArrayList<>())
                .columns(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        KanbanBoard savedBoard = kanbanBoardRepository.save(kanbanBoard);
        createDefaultColumns(savedBoard);
    }

    private void createDefaultColumns(KanbanBoard board) {
        kanbanColumnRepository.saveAll(List.of(
                buildColumn("Todo",        0, board),
                buildColumn("In Progress", 1, board),
                buildColumn("Done",        2, board)
        ));
    }

    private KanbanColumn buildColumn(String name, int position, KanbanBoard board) {
        return KanbanColumn.builder()
                .name(name)
                .position(position)
                .kanbanBoard(board)
                .issues(new ArrayList<>())
                .build();
    }

    public KanbanBoard getKanbanBoard(Long id) {
        return kanbanBoardRepository.findById(id).orElseThrow(
                () -> new DataNotFoundException("Kanban Board with id " + id + " not found"));
    }

}
