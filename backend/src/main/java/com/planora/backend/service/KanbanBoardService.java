package com.planora.backend.service;

import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.exception.UnauthorizedException;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.model.kanban.InvitedStatus;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.kanban.KanbanMember;
import com.planora.backend.model.kanban.dto.KanbanBoardRequest;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.IssueRepository;
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

    public void createKanbanBoard(String token, KanbanBoardRequest request, Long userId) {
        if (!githubService.checkIfRepositoryAndOwnerNameAreValid(token, request.githubOwnerName(), request.githubRepository())) {
            throw new DataNotFoundException("Repository " + request.githubRepository() + " not found for owner " + request.githubOwnerName());
        }

        User user = userService.findById(userId);

        KanbanBoard kanbanBoard = KanbanBoard.builder()
                .name(request.name())
                .description(request.description())
                .owner(user)
                .githubRepository(request.githubRepository())
                .githubOwnerName(request.githubOwnerName())
                .members(new ArrayList<>())
                .columns(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        addOwnerMemberInKanban(kanbanBoard, user);
        KanbanBoard savedBoard = kanbanBoardRepository.save(kanbanBoard);
        createDefaultColumns(savedBoard);
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

    public KanbanBoard getKanbanBoard(Long id) {
        return kanbanBoardRepository.findById(id).orElseThrow(
                () -> new DataNotFoundException("Kanban Board with id " + id + " not found"));
    }

}
