package com.planora.backend.controller;

import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.model.kanban.dto.KanbanBoardRequest;
import com.planora.backend.model.kanban.dto.KanbanBoardResponse;
import com.planora.backend.service.KanbanBoardService;
import com.planora.backend.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/kanban")
public class KanbanController {

    private final KanbanBoardService kanbanBoardService;
    private final TokenService tokenService;

    @PostMapping("/board")
    public ResponseEntity<KanbanBoardResponse> createKanbanBoard(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody KanbanBoardRequest request) {
        KanbanBoardResponse response = kanbanBoardService.createKanbanBoard(request, tokenService.getUserId(jwt), tokenService.getGithubToken(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/board")
    public ResponseEntity<List<KanbanBoardResponse>> getAllBoardsByUser(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(kanbanBoardService.getAllBoardsByUser(tokenService.getUserId(jwt)));
    }

    @GetMapping("/board/{id}")
    public ResponseEntity<KanbanBoardResponse> getBoardById(@PathVariable Long id) {
        return ResponseEntity.ok(kanbanBoardService.getBoardById(id));
    }

    @PutMapping("/board/{id}")
    public ResponseEntity<KanbanBoardResponse> updateBoard(
            @PathVariable Long id,
            @RequestBody KanbanBoardRequest request) {
        return ResponseEntity.ok(kanbanBoardService.updateBoard(id, request));
    }

    @DeleteMapping("/board/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {
        kanbanBoardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
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
