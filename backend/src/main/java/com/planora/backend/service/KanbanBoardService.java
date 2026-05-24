package com.planora.backend.service;

import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.model.issue.dto.IssueUpdateRequest;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanMember;
import com.planora.backend.model.kanban.dto.InvitedStatus;
import com.planora.backend.model.kanban.dto.KanbanBoardRequest;
import com.planora.backend.model.kanban.dto.KanbanBoardResponse;
import com.planora.backend.model.kanban.dto.KanbanColumnRequest;
import com.planora.backend.model.kanban.dto.KanbanColumnResponse;
import com.planora.backend.model.kanban.dto.KanbanColumnWithIssuesResponse;
import com.planora.backend.model.kanban.dto.KanbanMemberResponse;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.KanbanBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class KanbanBoardService implements IKanbanBoardService {

    private final KanbanBoardRepository kanbanBoardRepository;
    private final UserService userService;
    private final GithubService githubService;
    private final KanbanWebhookService kanbanWebhookService;
    private final IKanbanColumnService kanbanColumnService;
    private final KanbanIssueService kanbanIssueService;

    public KanbanBoardResponse createKanbanBoard(KanbanBoardRequest request, Long userId, String githubToken) {
        User user = userService.findById(userId);

        String[] repoParts = request.githubRepository().split("/");
        if (repoParts.length != 2) {
            throw new IllegalArgumentException("Formato inválido. Use owner/repository");
        }

        String owner = repoParts[0];
        String repo = repoParts[1];

        if (!githubService.checkIfRepositoryAndOwnerNameAreValid(githubToken, owner, repo)) {
            throw new DataNotFoundException("Repository " + request.githubRepository());
        }

        KanbanBoard kanbanBoard = KanbanBoard.builder()
                .name(request.name())
                .description(request.description())
                .owner(user)
                .githubRepository(repo)
                .githubOwnerName(owner)
                .members(new ArrayList<>())
                .columns(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        addOwnerMemberInKanban(kanbanBoard, user);
        KanbanBoard savedBoard = kanbanBoardRepository.save(kanbanBoard);
        kanbanColumnService.createDefaultColumns(savedBoard);
        kanbanWebhookService.registerWebhookIfNeeded(savedBoard, githubToken, owner, repo);
        return getBoardById(savedBoard.getKanbanBoardId());
    }

    public List<KanbanBoardResponse> getAllBoardsByUser(Long userId) {
        return kanbanBoardRepository.findBoardsByMemberUserIdAndStatus(userId, InvitedStatus.ACCEPTED).stream()
                .map(this::toResponse)
                .toList();
    }

    public KanbanBoardResponse getBoardById(Long id) {
        return toResponse(findById(id));
    }

    public KanbanBoardResponse updateBoard(Long id, KanbanBoardRequest request) {
        KanbanBoard board = findById(id);
        board.setName(request.name());
        board.setDescription(request.description());
        return toResponse(kanbanBoardRepository.save(board));
    }

    public void deleteBoard(Long id) {
        KanbanBoard board = findById(id);
        kanbanWebhookService.removeWebhookIfLastBoard(board);
        kanbanBoardRepository.delete(board);
    }

    public KanbanBoard findById(Long id) {
        return kanbanBoardRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Kanban Board with id " + id + " not found"));
    }

    public KanbanBoardResponse registerWebhook(Long boardId, String githubToken) {
        return toResponse(kanbanWebhookService.registerWebhook(boardId, githubToken));
    }

    public KanbanColumnResponse createColumn(Long boardId, KanbanColumnRequest request) {
        return kanbanColumnService.createColumn(boardId, request);
    }

    public KanbanColumnResponse updateColumn(Long boardId, Long columnId, String name, Integer position, Long userId) {
        return kanbanColumnService.updateColumn(boardId, columnId, name, position, userId);
    }

    public void deleteColumn(Jwt token, Long boardId, Long columnId, Long userId) {
        kanbanColumnService.deleteColumn(token, boardId, columnId, userId);
    }

    public List<KanbanColumnResponse> getColumns(Long boardId, Long userId) {
        return kanbanColumnService.getColumns(boardId, userId);
    }

    public List<KanbanColumnWithIssuesResponse> getColumnsWithIssues(Long boardId, Long userId) {
        return kanbanColumnService.getColumnsWithIssues(boardId, userId);
    }

    public IssueResponse createIssueAndAddToColumn(Long boardId, Long columnId, Jwt token,
                                                   IssueRequest issueRequest, Long userId, String repository) {
        return kanbanIssueService.createIssueAndAddToColumn(boardId, columnId, token, issueRequest, userId, repository);
    }

    public List<IssueResponse> createBulkIssuesAndAddToColumn(Long boardId, Long columnId, Jwt token,
                                                              List<IssueRequest> issueRequests, Long userId,
                                                              String repository) {
        return kanbanIssueService.createBulkIssuesAndAddToColumn(boardId, columnId, token, issueRequests, userId, repository);
    }

    public IssueResponse openIssue(Jwt token, Long issueId) {
        return kanbanIssueService.openIssue(token, issueId);
    }

    public IssueResponse closeIssue(Jwt token, Long issueId) {
        return kanbanIssueService.closeIssue(token, issueId);
    }

    public IssueResponse updateIssue(Jwt token, Long issueId, IssueUpdateRequest request) {
        return kanbanIssueService.updateIssue(token, issueId, request);
    }

    public void deleteIssue(Jwt token, Long issueId) {
        kanbanIssueService.deleteIssue(token, issueId);
    }

    public void moveIssue(Long boardId, Long issueId, Long targetColumnId, Long userId) {
        kanbanIssueService.moveIssue(boardId, issueId, targetColumnId, userId);
    }

    private static void addOwnerMemberInKanban(KanbanBoard kanbanBoard, User user) {
        KanbanMember ownerMember = KanbanMember.builder()
                .kanbanBoard(kanbanBoard)
                .user(user)
                .invitedAt(LocalDateTime.now())
                .joinedAt(LocalDateTime.now())
                .invitedStatus(InvitedStatus.ACCEPTED)
                .build();
        kanbanBoard.getMembers().add(ownerMember);
    }

    private KanbanBoardResponse toResponse(KanbanBoard board) {
        List<KanbanColumnResponse> columns = board.getColumns().stream()
                .map(c -> new KanbanColumnResponse(c.getKanbanColumnId(), c.getName(), c.getPosition()))
                .toList();

        List<KanbanMemberResponse> members = board.getMembers().stream()
                .map(m -> new KanbanMemberResponse(
                        m.getKanbanMemberId(),
                        m.getUser().getLogin(),
                        m.getUser().getAvatarUrl(),
                        m.getInvitedStatus(),
                        m.getInvitedAt(),
                        m.getJoinedAt()))
                .toList();

        return new KanbanBoardResponse(
                board.getKanbanBoardId(),
                board.getName(),
                board.getDescription(),
                board.getGithubRepository(),
                board.getGithubOwnerName(),
                board.getOwner().getLogin(),
                board.getCreatedAt(),
                board.getGithubWebhookId() != null,
                columns,
                members
        );
    }
}
