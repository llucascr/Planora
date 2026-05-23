package com.planora.backend.controller;

import com.planora.backend.model.issue.dto.BulkIssueRequest;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.model.issue.dto.MoveIssueRequest;
import com.planora.backend.model.issue.dto.IssueUpdateRequest;
import com.planora.backend.model.kanban.dto.*;
import com.planora.backend.service.IKanbanBoardService;
import com.planora.backend.service.KanbanMemberService;
import com.planora.backend.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/kanban")
public class KanbanController {

    private final IKanbanBoardService kanbanBoardService;
    private final KanbanMemberService kanbanMemberService;
    private final TokenService tokenService;

    @PostMapping("/board/create")
    public ResponseEntity<KanbanBoardResponse> createKanbanBoard(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody KanbanBoardRequest request) {
        KanbanBoardResponse response = kanbanBoardService.createKanbanBoard(request, tokenService.getUserId(jwt), tokenService.getGithubToken(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/board/list")
    public ResponseEntity<List<KanbanBoardResponse>> getAllBoardsByUser(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(kanbanBoardService.getAllBoardsByUser(tokenService.getUserId(jwt)));
    }

    @GetMapping("/board/{id}")
    public ResponseEntity<KanbanBoardResponse> getBoardById(@PathVariable Long id) {
        return ResponseEntity.ok(kanbanBoardService.getBoardById(id));
    }

    @PutMapping("/board/update/{id}")
    public ResponseEntity<KanbanBoardResponse> updateBoard(
            @PathVariable Long id,
            @RequestBody KanbanBoardRequest request) {
        return ResponseEntity.ok(kanbanBoardService.updateBoard(id, request));
    }

    @DeleteMapping("/board/delete/{id}")
    public ResponseEntity<Map<String, String>> deleteBoard(@PathVariable Long id) {
        kanbanBoardService.deleteBoard(id);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Board deleted successfully"));
    }

    @PostMapping("/board/{boardId}/webhook")
    public ResponseEntity<KanbanBoardResponse> registerWebhook(
            @PathVariable Long boardId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(kanbanBoardService.registerWebhook(boardId, tokenService.getGithubToken(jwt)));
    }

    @PostMapping("/board/issue/create")
    public ResponseEntity<IssueResponse> createIssueAndAddToColumn(
            @RequestParam Long boardId,
            @RequestParam Long columnId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody IssueRequest issueRequest,
            @RequestParam String repository
    ) {
        Long userId = tokenService.getUserId(jwt);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                kanbanBoardService.createIssueAndAddToColumn(boardId, columnId, jwt, issueRequest, userId, repository)
        );
    }

    @PostMapping("/board/issue/bulk")
    public ResponseEntity<List<IssueResponse>> createBulkIssuesAndAddToColumn(
            @RequestParam Long boardId,
            @RequestParam Long columnId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BulkIssueRequest bulkRequest,
            @RequestParam Long userId,
            @RequestParam String repository
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                kanbanBoardService.createBulkIssuesAndAddToColumn(boardId, columnId, jwt, bulkRequest.issues(), userId, repository)
        );
    }

    @PatchMapping("/board/issue/{issueId}/open")
    public ResponseEntity<IssueResponse> openIssue(
            @PathVariable Long issueId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(kanbanBoardService.openIssue(jwt, issueId));
    }

    @PatchMapping("/board/issue/{issueId}/close")
    public ResponseEntity<IssueResponse> closeIssue(
            @PathVariable Long issueId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(kanbanBoardService.closeIssue(jwt, issueId));
    }

    @DeleteMapping("/board/issue/{issueId}")
    public ResponseEntity<Map<String, String>> deleteIssue(
            @PathVariable Long issueId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        kanbanBoardService.deleteIssue(jwt, issueId);
        return ResponseEntity.ok(Map.of("message", "Issue deleted successfully"));
    }

    @PatchMapping("/board/issue/{issueId}")
    public ResponseEntity<IssueResponse> updateIssue(
            @PathVariable Long issueId,
            @RequestBody IssueUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(kanbanBoardService.updateIssue(jwt, issueId, request));
    }

    @PostMapping("/board/{boardId}/member/invite")
    public ResponseEntity<KanbanMemberResponse> inviteMember(
            @PathVariable Long boardId,
            @RequestBody MemberInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kanbanMemberService.inviteMember(boardId, request));
    }

    @GetMapping("/board/{boardId}/member/list")
    public ResponseEntity<List<KanbanMemberResponse>> getMembersByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(kanbanMemberService.getMembersByBoard(boardId));
    }

    @PatchMapping("/member/{memberId}/status/update")
    public ResponseEntity<KanbanMemberResponse> updateMemberStatus(
            @PathVariable Long memberId,
            @RequestParam String status) {
        return ResponseEntity.ok(kanbanMemberService.updateMemberStatus(memberId, status));
    }

    @DeleteMapping("/board/{boardId}/member/delete/{memberId}")
    public ResponseEntity<Map<String, String>> removeMember(
            @PathVariable Long boardId,
            @PathVariable Long memberId) {
        kanbanMemberService.removeMember(boardId, memberId);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Member deleted successfully"));
    }

    @PostMapping("/board/{boardId}/column")
    public ResponseEntity<KanbanColumnResponse> createColumn(
            @PathVariable Long boardId,
            @RequestBody KanbanColumnRequest request
    ) {
        KanbanColumnResponse response =
                kanbanBoardService.createColumn(boardId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/board/{boardId}/column/{columnId}")
    public ResponseEntity<KanbanColumnResponse> updateColumn(
            @PathVariable Long boardId,
            @PathVariable Long columnId,
            @RequestBody UpdateColumnRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = tokenService.getUserId(jwt);

        return ResponseEntity.ok(
                kanbanBoardService.updateColumn(
                        boardId,
                        columnId,
                        request.name(),
                        request.position(),
                        userId
                )
        );
    }

    @DeleteMapping("/board/{boardId}/column/{columnId}")
    public ResponseEntity<Map<String, String>> deleteColumn(
            @PathVariable Long boardId,
            @PathVariable Long columnId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = tokenService.getUserId(jwt);

        kanbanBoardService.deleteColumn(jwt, boardId, columnId, userId);

        return ResponseEntity.ok(Map.of("message", "Column deleted successfully"));
    }

    @GetMapping("/board/{boardId}/columns")
    public ResponseEntity<List<KanbanColumnResponse>> getColumns(
            @PathVariable Long boardId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = tokenService.getUserId(jwt);

        return ResponseEntity.ok(
                kanbanBoardService.getColumns(boardId, userId)
        );
    }

    @PatchMapping("/board/{boardId}/issue/move")
    public ResponseEntity<Map<String, String>> moveIssue(
            @PathVariable Long boardId,
            @RequestBody MoveIssueRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = tokenService.getUserId(jwt);

        kanbanBoardService.moveIssue(
                boardId,
                request.issueId(),
                request.targetColumnId(),
                userId
        );

        return ResponseEntity.ok(Map.of("message", "Issue moved successfully"));
    }

    @GetMapping("/board/{boardId}/columns/issues")
    public ResponseEntity<List<KanbanColumnWithIssuesResponse>> getColumnsWithIssues(
            @PathVariable Long boardId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = tokenService.getUserId(jwt);

        return ResponseEntity.ok(
                kanbanBoardService.getColumnsWithIssues(boardId, userId)
        );
    }
}
