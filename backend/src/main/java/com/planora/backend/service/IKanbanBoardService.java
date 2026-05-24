package com.planora.backend.service;

import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.model.issue.dto.IssueUpdateRequest;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.dto.KanbanBoardRequest;
import com.planora.backend.model.kanban.dto.KanbanBoardResponse;
import com.planora.backend.model.kanban.dto.KanbanColumnRequest;
import com.planora.backend.model.kanban.dto.KanbanColumnResponse;
import com.planora.backend.model.kanban.dto.KanbanColumnWithIssuesResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface IKanbanBoardService {

    KanbanBoardResponse createKanbanBoard(KanbanBoardRequest request, Long userId, String githubToken);

    List<KanbanBoardResponse> getAllBoardsByUser(Long userId);

    KanbanBoardResponse getBoardById(Long id);

    KanbanBoardResponse updateBoard(Long id, KanbanBoardRequest request);

    void deleteBoard(Long id);

    KanbanBoard findById(Long id);

    KanbanBoardResponse registerWebhook(Long boardId, String githubToken);

    KanbanColumnResponse createColumn(Long boardId, KanbanColumnRequest request);

    KanbanColumnResponse updateColumn(Long boardId, Long columnId, String name, Integer position, Long userId);

    void deleteColumn(Jwt token, Long boardId, Long columnId, Long userId);

    List<KanbanColumnResponse> getColumns(Long boardId, Long userId);

    List<KanbanColumnWithIssuesResponse> getColumnsWithIssues(Long boardId, Long userId);

    IssueResponse createIssueAndAddToColumn(Long boardId, Long columnId, Jwt token,
                                            IssueRequest issueRequest, Long userId, String repository);

    List<IssueResponse> createBulkIssuesAndAddToColumn(Long boardId, Long columnId, Jwt token,
                                                       List<IssueRequest> issueRequests, Long userId,
                                                       String repository);

    IssueResponse openIssue(Jwt token, Long issueId);

    IssueResponse closeIssue(Jwt token, Long issueId);

    IssueResponse updateIssue(Jwt token, Long issueId, IssueUpdateRequest request);

    void deleteIssue(Jwt token, Long issueId);

    void moveIssue(Long boardId, Long issueId, Long targetColumnId, Long userId);
}
