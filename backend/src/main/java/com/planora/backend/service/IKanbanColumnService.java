package com.planora.backend.service;

import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.dto.KanbanColumnRequest;
import com.planora.backend.model.kanban.dto.KanbanColumnResponse;
import com.planora.backend.model.kanban.dto.KanbanColumnWithIssuesResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface IKanbanColumnService {

    void createDefaultColumns(KanbanBoard board);

    KanbanColumnResponse createColumn(Long boardId, KanbanColumnRequest request);

    KanbanColumnResponse updateColumn(Long boardId, Long columnId, String name, Integer position, Long userId);

    void deleteColumn(Jwt token, Long boardId, Long columnId, Long userId);

    List<KanbanColumnResponse> getColumns(Long boardId, Long userId);

    List<KanbanColumnWithIssuesResponse> getColumnsWithIssues(Long boardId, Long userId);
}
