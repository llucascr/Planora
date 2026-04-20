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

        String githubOwnerName = user.getLogin();

        if (!githubService.checkIfRepositoryAndOwnerNameAreValid(githubToken, githubOwnerName, request.githubRepository())) {
            throw new DataNotFoundException("Repository " + request.githubRepository() + " not found for owner " + githubOwnerName);
        }

        KanbanBoard kanbanBoard = KanbanBoard.builder()
                .name(request.name())
                .description(request.description())
                .owner(user)
                .githubRepository(request.githubRepository())
                .githubOwnerName(githubOwnerName)
                .members(new ArrayList<>())
                .columns(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        addMemberInKanban(kanbanBoard, user);
        KanbanBoard savedBoard = kanbanBoardRepository.save(kanbanBoard);
        createDefaultColumns(savedBoard);
        return getBoardById(savedBoard.getKanbanBoardId());
    }

    public IssueResponse createIssueAndAddToColumn(Long boardId, Long columnId, String token, IssueRequest issueRequest,
                                                   Long userId, String repository
    ) {
        KanbanBoard board = getKanbanBoard(boardId);

        User user = userService.findById(userId);

        if (kanbanMemberRepository.findByKanbanBoardAndUser(board, user).isEmpty()) {
            throw new UnauthorizedException("Kanban member not found");
        }

        KanbanColumn column = board.getColumns().stream()
                .filter(c -> c.getKanbanColumnId().equals(columnId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Column with id " + columnId + " not found in board " + boardId));

        return githubService.createIssue(token, issueRequest, userId, repository, column);
    }

    public KanbanBoard getKanbanBoard(Long id) {
        return kanbanBoardRepository.findById(id).orElseThrow(
                () -> new DataNotFoundException("Kanban Board with id " + id + " not found"));
    }

    public List<KanbanBoardResponse> getAllBoardsByUser(Long userId) {
        return kanbanBoardRepository.findByOwner_UserId(userId).stream()
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

    private static void addMemberInKanban(KanbanBoard kanbanBoard, User user) {
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
                buildColumn("Todo",        0, board),
                buildColumn("In Progress", 1, board),
                buildColumn("Done",        2, board)
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
}
