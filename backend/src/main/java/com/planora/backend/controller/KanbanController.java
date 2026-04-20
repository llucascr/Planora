package com.planora.backend.controller;

import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.model.kanban.dto.KanbanBoardRequest;
import com.planora.backend.model.kanban.dto.KanbanBoardResponse;
import com.planora.backend.model.kanban.dto.KanbanMemberResponse;
import com.planora.backend.model.kanban.dto.MemberInviteRequest;
import com.planora.backend.service.KanbanBoardService;
import com.planora.backend.service.KanbanMemberService;
import com.planora.backend.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/kanban")
public class KanbanController {

    private final KanbanBoardService kanbanBoardService;
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

    @PostMapping("/board/issue/create")
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
}
