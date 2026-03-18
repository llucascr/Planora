package com.planora.backend.service;

import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.dto.KanbanBoardRequest;
import com.planora.backend.repository.KanbanBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

@RequiredArgsConstructor
@Service
public class KanbanBoardService {

    private final KanbanBoardRepository kanbanBoardRepository;

    private final UserService userService;

    public void createKanbanBoard(KanbanBoardRequest request, Long userId) {
        KanbanBoard kanbanBoard = KanbanBoard.builder()
                .name(request.name())
                .description(request.description())
                .owner(userService.findById(userId))
                // Verificar se esse repository existe nesse ownerName
                .githubRepository(request.githubRepository())
                .githubOwnerName(request.githubOwnerName())
                .members(new ArrayList<>())
                .columns(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        kanbanBoardRepository.save(kanbanBoard);
    }

    public KanbanBoard getKanbanBoard(Long id) {
        return kanbanBoardRepository.findById(id).orElseThrow(
                () -> new DataNotFoundException("Kanban Board with id " + id + " not found"));
    }

}
