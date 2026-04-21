package com.planora.backend.service;

import com.planora.backend.exception.DataAlreadyExistException;
import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.kanban.dto.InvitedStatus;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanMember;
import com.planora.backend.model.kanban.dto.KanbanMemberResponse;
import com.planora.backend.model.kanban.dto.MemberInviteRequest;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.KanbanMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class KanbanMemberService {

    private final KanbanMemberRepository kanbanMemberRepository;
    private final KanbanBoardService kanbanBoardService;
    private final UserService userService;

    public KanbanMemberResponse inviteMember(Long boardId, MemberInviteRequest request) {
        KanbanBoard board = kanbanBoardService.findById(boardId);
        User user = userService.findByLogin(request.login());

        kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(boardId, user.getUserId())
                .ifPresent(m -> { throw new DataAlreadyExistException("User " + request.login() + " is already a member of this board"); });

        KanbanMember member = KanbanMember.builder()
                .kanbanBoard(board)
                .user(user)
                .invitedAt(LocalDateTime.now())
                .invitedStatus(InvitedStatus.PENDING)
                .build();

        return toResponse(kanbanMemberRepository.save(member));
    }

    public List<KanbanMemberResponse> getMembersByBoard(Long boardId) {
        kanbanBoardService.findById(boardId);
        return kanbanMemberRepository.findByKanbanBoard_KanbanBoardId(boardId).stream()
                .map(this::toResponse)
                .toList();
    }

    public KanbanMemberResponse updateMemberStatus(Long memberId, String status) {
        InvitedStatus invitedStatus = InvitedStatus.valueOf(status.toUpperCase());
        KanbanMember member = findById(memberId);
        member.setInvitedStatus(invitedStatus);
        if (invitedStatus == InvitedStatus.ACCEPTED) {
            member.setJoinedAt(LocalDateTime.now());
        }
        return toResponse(kanbanMemberRepository.save(member));
    }

    public void removeMember(Long boardId, Long memberId) {
        KanbanMember member = findById(memberId);
        if (!member.getKanbanBoard().getKanbanBoardId().equals(boardId)) {
            throw new DataNotFoundException("Member with id " + memberId + " not found in board " + boardId);
        }
        kanbanMemberRepository.delete(member);
    }

    private KanbanMember findById(Long memberId) {
        return kanbanMemberRepository.findById(memberId).orElseThrow(
                () -> new DataNotFoundException("Member with id " + memberId + " not found"));
    }

    private KanbanMemberResponse toResponse(KanbanMember member) {
        return new KanbanMemberResponse(
                member.getKanbanMemberId(),
                member.getUser().getLogin(),
                member.getUser().getAvatarUrl(),
                member.getInvitedStatus(),
                member.getInvitedAt(),
                member.getJoinedAt()
        );
    }
}
