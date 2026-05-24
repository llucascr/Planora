package com.planora.backend.service;

import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.exception.UnauthorizedException;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.dto.IssueSummaryResponse;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.kanban.dto.KanbanColumnRequest;
import com.planora.backend.model.kanban.dto.KanbanColumnResponse;
import com.planora.backend.model.kanban.dto.KanbanColumnWithIssuesResponse;
import com.planora.backend.repository.KanbanBoardRepository;
import com.planora.backend.repository.KanbanColumnRepository;
import com.planora.backend.repository.KanbanMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class KanbanColumnService implements IKanbanColumnService {

    private final KanbanBoardRepository kanbanBoardRepository;
    private final KanbanColumnRepository kanbanColumnRepository;
    private final KanbanMemberRepository kanbanMemberRepository;
    private final GithubService githubService;

    public void createDefaultColumns(KanbanBoard board) {
        kanbanColumnRepository.saveAll(List.of(
                buildColumn("Todo", 0, board),
                buildColumn("In Progress", 1, board),
                buildColumn("Done", 2, board)
        ));
    }

    public KanbanColumnResponse createColumn(Long boardId, KanbanColumnRequest request) {
        KanbanBoard board = kanbanBoardRepository.findById(boardId)
                .orElseThrow(() -> new DataNotFoundException("Kanban Board with id " + boardId + " not found"));

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

    public KanbanColumnResponse updateColumn(Long boardId, Long columnId, String name, Integer position, Long userId) {
        validateBoardMember(boardId, userId);

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

    public void deleteColumn(Jwt token, Long boardId, Long columnId, Long userId) {
        KanbanColumn column = kanbanColumnRepository.findById(columnId)
                .orElseThrow(() -> new DataNotFoundException("Column not found"));

        if (!column.getKanbanBoard().getKanbanBoardId().equals(boardId)) {
            throw new DataNotFoundException("Column does not belong to this board");
        }

        List<Issue> issues = new ArrayList<>(column.getIssues());
        for (Issue issue : issues) {
            githubService.deleteIssue(token, issue.getIssueId());
        }

        kanbanColumnRepository.delete(column);

        List<KanbanColumn> remaining =
                kanbanColumnRepository.findByKanbanBoard_KanbanBoardIdOrderByPosition(boardId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i);
        }
        kanbanColumnRepository.saveAll(remaining);
    }

    public List<KanbanColumnResponse> getColumns(Long boardId, Long userId) {
        validateBoardMember(boardId, userId);

        return kanbanColumnRepository.findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(boardId).stream()
                .map(col -> new KanbanColumnResponse(col.getKanbanColumnId(), col.getName(), col.getPosition()))
                .toList();
    }

    public List<KanbanColumnWithIssuesResponse> getColumnsWithIssues(Long boardId, Long userId) {
        validateBoardMember(boardId, userId);

        return kanbanColumnRepository.findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(boardId).stream()
                .map(col -> new KanbanColumnWithIssuesResponse(
                        col.getKanbanColumnId(),
                        col.getName(),
                        col.getPosition(),
                        col.getIssues().stream().map(IssueSummaryResponse::fromEntity).toList()
                ))
                .toList();
    }

    private void validateBoardMember(Long boardId, Long userId) {
        if (kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(boardId, userId).isEmpty()) {
            throw new UnauthorizedException("Kanban member not found");
        }
    }

    private KanbanColumn buildColumn(String name, int position, KanbanBoard board) {
        return KanbanColumn.builder()
                .name(name)
                .position(position)
                .kanbanBoard(board)
                .issues(new ArrayList<>())
                .build();
    }
}
