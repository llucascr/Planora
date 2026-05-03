package com.planora.backend.service;

import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.exception.UnauthorizedException;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.kanban.KanbanMember;
import com.planora.backend.model.kanban.dto.*;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.KanbanBoardRepository;
import com.planora.backend.repository.KanbanColumnRepository;
import com.planora.backend.repository.KanbanMemberRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class KanbanBoardService {

    private static final Logger log = LoggerFactory.getLogger(KanbanBoardService.class);

    private final KanbanBoardRepository kanbanBoardRepository;
    private final KanbanColumnRepository kanbanColumnRepository;
    private final KanbanMemberRepository kanbanMemberRepository;
    private final UserService userService;
    private final GithubService githubService;

    public KanbanBoardResponse createKanbanBoard(KanbanBoardRequest request, Long userId, String githubToken) {
        User user = userService.findById(userId);

        String[] repoParts = request.githubRepository().split("/");

        if (repoParts.length != 2) {
            throw new IllegalArgumentException("Formato inválido. Use owner/repository");
        }

        String owner = repoParts[0];
        String repo = repoParts[1];

        if (!githubService.checkIfRepositoryAndOwnerNameAreValid(
                githubToken,
                owner,
                repo
        )) {
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
        createDefaultColumns(savedBoard);
        return getBoardById(savedBoard.getKanbanBoardId());
    }

    public IssueResponse createIssueAndAddToColumn(Long boardId, Long columnId, Jwt token, IssueRequest issueRequest,
                                                   Long userId, String repository
    ) {

        if (kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(boardId, userId).isEmpty()) {
            throw new UnauthorizedException("Kanban member not found");
        }

        KanbanColumn column = getKanbanBoard(boardId).getColumns().stream()
                .filter(c -> c.getKanbanColumnId().equals(columnId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Column with id " + columnId + " not found in board " + boardId));

        return githubService.createIssue(token, issueRequest, userId, repository, column);
    }

    public List<IssueResponse> createBulkIssuesAndAddToColumn(Long boardId, Long columnId, Jwt token,
                                                              List<IssueRequest> issueRequests, Long userId, String repository
    ) {

        if (kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(boardId, userId).isEmpty()) {
            throw new UnauthorizedException("Kanban member not found");
        }

        KanbanColumn column = getKanbanBoard(boardId).getColumns().stream()
                .filter(c -> c.getKanbanColumnId().equals(columnId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Column with id " + columnId + " not found in board " + boardId));

        return githubService.createBulkIssues(token, issueRequests, userId, repository, column);
    }

    public KanbanBoard getKanbanBoard(Long id) {
        return kanbanBoardRepository.findById(id).orElseThrow(
                () -> new DataNotFoundException("Kanban Board with id " + id + " not found"));
    }

    public List<KanbanBoardResponse> getAllBoardsByUser(Long userId) {
        return kanbanBoardRepository.findByMembers_User_UserIdAndMembers_InvitedStatus(userId, InvitedStatus.ACCEPTED).stream()
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
        kanbanBoardRepository.delete(board);
    }

    public KanbanBoard findById(Long id) {
        return kanbanBoardRepository.findById(id).orElseThrow(
                () -> new DataNotFoundException("Kanban Board with id " + id + " not found"));
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

    private void createDefaultColumns(KanbanBoard board) {
        kanbanColumnRepository.saveAll(List.of(
                buildColumn("Todo", 0, board),
                buildColumn("In Progress", 1, board),
                buildColumn("Done", 2, board)
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
                columns,
                members
        );
    }

    public KanbanColumnResponse createColumn(Long boardId, KanbanColumnRequest request) {

        KanbanBoard board = kanbanBoardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));

        KanbanColumn column = KanbanColumn.builder()
                .name(request.name())
                .position(board.getColumns().size())
                .kanbanBoard(board)
                .build();

        kanbanColumnRepository.save(column);

        return new KanbanColumnResponse(
                column.getKanbanColumnId(),
                column.getName(),
                column.getPosition()
        );
    }

    public KanbanColumnResponse updateColumn(
            Long boardId,
            Long columnId,
            String name,
            Integer position,
            Long userId
    ) {
        if (kanbanMemberRepository
                .findByKanbanBoard_KanbanBoardIdAndUser_UserId(boardId, userId)
                .isEmpty()) {
            throw new UnauthorizedException("Kanban member not found");
        }

        KanbanColumn column = kanbanColumnRepository.findById(columnId)
                .orElseThrow(() -> new DataNotFoundException("Column not found"));

        if (!column.getKanbanBoard().getKanbanBoardId().equals(boardId)) {
            throw new DataNotFoundException("Column does not belong to this board");
        }

        if (name != null && !name.isBlank()) {
            column.setName(name);
        }

        if (position != null) {
            List<KanbanColumn> columns =
                    kanbanColumnRepository.findByKanbanBoard_KanbanBoardIdOrderByPosition(boardId);

            columns.removeIf(c -> c.getKanbanColumnId().equals(columnId));
            columns.add(position, column);

            for (int i = 0; i < columns.size(); i++) {
                columns.get(i).setPosition(i);
            }

            kanbanColumnRepository.saveAll(columns);
        } else {
            kanbanColumnRepository.save(column);
        }

        return new KanbanColumnResponse(
                column.getKanbanColumnId(),
                column.getName(),
                column.getPosition()
        );
    }

    public void deleteColumn(Long boardId, Long columnId, Long userId) {

        if (kanbanMemberRepository
                .findByKanbanBoard_KanbanBoardIdAndUser_UserId(boardId, userId)
                .isEmpty()) {
            throw new UnauthorizedException("Kanban member not found");
        }

        KanbanColumn column = kanbanColumnRepository.findById(columnId)
                .orElseThrow(() -> new DataNotFoundException("Column not found"));

        if (!column.getKanbanBoard().getKanbanBoardId().equals(boardId)) {
            throw new DataNotFoundException("Column does not belong to this board");
        }

        kanbanColumnRepository.delete(column);

        List<KanbanColumn> columns =
                kanbanColumnRepository.findByKanbanBoard_KanbanBoardIdOrderByPosition(boardId);

        for (int i = 0; i < columns.size(); i++) {
            columns.get(i).setPosition(i);
        }

        kanbanColumnRepository.saveAll(columns);
    }

    public List<KanbanColumnResponse> getColumns(Long boardId, Long userId) {

        if (kanbanMemberRepository
                .findByKanbanBoard_KanbanBoardIdAndUser_UserId(boardId, userId)
                .isEmpty()) {
            throw new UnauthorizedException("Kanban member not found");
        }

        List<KanbanColumn> columns =
                kanbanColumnRepository.findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(boardId);

        return columns.stream()
                .map(col -> new KanbanColumnResponse(
                        col.getKanbanColumnId(),
                        col.getName(),
                        col.getPosition()
                ))
                .toList();
    }
}
