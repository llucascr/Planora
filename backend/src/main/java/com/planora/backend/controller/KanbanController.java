package com.planora.backend.controller;

import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.kanban.dto.KanbanBoardRequest;
import com.planora.backend.repository.KanbanBoardRepository;
import com.planora.backend.service.KanbanBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/kanban")
public class KanbanController {

    private final KanbanBoardService kanbanBoardService;

    @PostMapping("/board")
    public ResponseEntity<?> createKanbanBoard(
            @RequestHeader("token") String token,
            @RequestBody KanbanBoardRequest request,
            @RequestParam Long userId) {
        kanbanBoardService.createKanbanBoard(token, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/createIssue")
    public ResponseEntity<IssueResponse> createIssueAndAddToColumn(
            @RequestParam Long boardId,
            @RequestParam Long columnId,
            @RequestHeader("token") String token,
            @RequestBody IssueRequest issueRequest,
            @RequestParam Long userId,
            @RequestParam String repository
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                kanbanBoardService.createIssueAndAddToColumn(boardId, columnId, token, issueRequest, userId, repository)
        );
    }

}
