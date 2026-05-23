package com.planora.backend.service;

import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.exception.UnauthorizedException;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.model.issue.dto.IssueUpdateRequest;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.repository.KanbanBoardRepository;
import com.planora.backend.repository.KanbanColumnRepository;
import com.planora.backend.repository.KanbanMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class KanbanIssueService {

    private final KanbanBoardRepository kanbanBoardRepository;
    private final KanbanColumnRepository kanbanColumnRepository;
    private final KanbanMemberRepository kanbanMemberRepository;
    private final GithubService githubService;

    public IssueResponse createIssueAndAddToColumn(Long boardId, Long columnId, Jwt token,
                                                   IssueRequest issueRequest, Long userId, String repository) {
        validateBoardMember(boardId, userId);

        KanbanColumn column = findColumnInBoard(boardId, columnId);
        return githubService.createIssue(token, issueRequest, userId, repository, column);
    }

    public List<IssueResponse> createBulkIssuesAndAddToColumn(Long boardId, Long columnId, Jwt token,
                                                              List<IssueRequest> issueRequests, Long userId,
                                                              String repository) {
        validateBoardMember(boardId, userId);

        KanbanColumn column = findColumnInBoard(boardId, columnId);
        return githubService.createBulkIssues(token, issueRequests, userId, repository, column);
    }

    public IssueResponse openIssue(Jwt token, Long issueId) {
        return githubService.openIssue(token, issueId);
    }

    public IssueResponse closeIssue(Jwt token, Long issueId) {
        return githubService.closeIssue(token, issueId);
    }

    public IssueResponse updateIssue(Jwt token, Long issueId, IssueUpdateRequest request) {
        return githubService.updateIssue(token, issueId, request);
    }

    public void deleteIssue(Jwt token, Long issueId) {
        githubService.deleteIssue(token, issueId);
    }

    public void moveIssue(Long boardId, Long issueId, Long targetColumnId, Long userId) {
        validateBoardMember(boardId, userId);

        KanbanBoard board = findBoardById(boardId);

        Issue issue = board.getColumns().stream()
                .flatMap(c -> c.getIssues().stream())
                .filter(i -> i.getIssueId().equals(issueId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Issue not found"));

        KanbanColumn currentColumn = issue.getColumn();

        KanbanColumn targetColumn = board.getColumns().stream()
                .filter(c -> c.getKanbanColumnId().equals(targetColumnId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Target column not found"));

        currentColumn.getIssues().remove(issue);
        issue.setColumn(targetColumn);

        kanbanColumnRepository.save(currentColumn);
        kanbanColumnRepository.save(targetColumn);
    }

    private void validateBoardMember(Long boardId, Long userId) {
        if (kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(boardId, userId).isEmpty()) {
            throw new UnauthorizedException("Kanban member not found");
        }
    }

    private KanbanBoard findBoardById(Long id) {
        return kanbanBoardRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Kanban Board with id " + id + " not found"));
    }

    private KanbanColumn findColumnInBoard(Long boardId, Long columnId) {
        return findBoardById(boardId).getColumns().stream()
                .filter(c -> c.getKanbanColumnId().equals(columnId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException(
                        "Column with id " + columnId + " not found in board " + boardId));
    }
}
