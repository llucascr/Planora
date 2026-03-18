package com.planora.backend.service;

import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.repository.KanbanColumnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class KanbanColumnService {

    private final KanbanColumnRepository kanbanColumnRepository;

    private final KanbanBoardService kanbanBoardService;

    public List<KanbanColumn> createDefaultColumns(Long kanbanId) {

        KanbanColumn kanbanColumnTodo = KanbanColumn.builder()
                .name("TODO")
                .position(0)
                .kanbanBoard(kanbanBoardService.getKanbanBoard(kanbanId))
                .issues(new ArrayList<>())
                .build();

        KanbanColumn kanbanColumnInProgress = KanbanColumn.builder()
                .name("InProgress")
                .position(1)
                .kanbanBoard(kanbanBoardService.getKanbanBoard(kanbanId))
                .issues(new ArrayList<>())
                .build();

        KanbanColumn kanbanColumnDone = KanbanColumn.builder()
                .name("Done")
                .position(2)
                .kanbanBoard(kanbanBoardService.getKanbanBoard(kanbanId))
                .issues(new ArrayList<>())
                .build();

        return kanbanColumnRepository.saveAll(List.of(kanbanColumnTodo, kanbanColumnInProgress, kanbanColumnDone));
    }

}
