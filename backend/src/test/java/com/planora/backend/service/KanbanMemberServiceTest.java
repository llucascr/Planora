package com.planora.backend.service;

import com.planora.backend.exception.DataAlreadyExistException;
import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanMember;
import com.planora.backend.model.kanban.dto.InvitedStatus;
import com.planora.backend.model.kanban.dto.KanbanMemberResponse;
import com.planora.backend.model.kanban.dto.MemberInviteRequest;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.KanbanMemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KanbanMemberService")
class KanbanMemberServiceTest {

    private static final Long BOARD_ID = 1L;
    private static final Long MEMBER_ID = 10L;
    private static final Long USER_ID = 42L;
    private static final String LOGIN = "alice";
    private static final String AVATAR_URL = "https://avatars.example.com/u/42";

    @Mock private KanbanMemberRepository kanbanMemberRepository;
    @Mock private KanbanBoardService kanbanBoardService;
    @Mock private UserService userService;

    @InjectMocks private KanbanMemberService kanbanMemberService;

    private User user;
    private KanbanBoard board;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(USER_ID)
                .login(LOGIN)
                .avatarUrl(AVATAR_URL)
                .build();

        board = KanbanBoard.builder()
                .kanbanBoardId(BOARD_ID)
                .build();
    }

    private KanbanMember buildMember(InvitedStatus status, KanbanBoard memberBoard) {
        return KanbanMember.builder()
                .kanbanMemberId(MEMBER_ID)
                .kanbanBoard(memberBoard)
                .user(user)
                .invitedAt(LocalDateTime.now().minusDays(1))
                .joinedAt(status == InvitedStatus.ACCEPTED ? LocalDateTime.now() : null)
                .invitedStatus(status)
                .build();
    }

    @Nested
    @DisplayName("inviteMember")
    class InviteMember {

        @Test
        @DisplayName("deve criar member com status PENDING quando usuário ainda não é membro")
        void deveCriarMemberComStatusPending_quandoUsuarioNaoEhMembro() {
            MemberInviteRequest request = new MemberInviteRequest(LOGIN);
            when(kanbanBoardService.findById(BOARD_ID)).thenReturn(board);
            when(userService.findByLogin(LOGIN)).thenReturn(user);
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.empty());
            when(kanbanMemberRepository.save(any(KanbanMember.class))).thenAnswer(invocation -> {
                KanbanMember m = invocation.getArgument(0);
                m.setKanbanMemberId(MEMBER_ID);
                return m;
            });

            KanbanMemberResponse response = kanbanMemberService.inviteMember(BOARD_ID, request);

            ArgumentCaptor<KanbanMember> memberCaptor = ArgumentCaptor.forClass(KanbanMember.class);
            verify(kanbanMemberRepository).save(memberCaptor.capture());
            KanbanMember saved = memberCaptor.getValue();

            assertThat(saved.getKanbanBoard()).isSameAs(board);
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getInvitedStatus()).isEqualTo(InvitedStatus.PENDING);
            assertThat(saved.getInvitedAt()).isNotNull();
            assertThat(saved.getJoinedAt()).isNull();

            assertThat(response.kanbanMemberId()).isEqualTo(MEMBER_ID);
            assertThat(response.login()).isEqualTo(LOGIN);
            assertThat(response.avatarUrl()).isEqualTo(AVATAR_URL);
            assertThat(response.invitedStatus()).isEqualTo(InvitedStatus.PENDING);
            assertThat(response.joinedAt()).isNull();
        }

        @Test
        @DisplayName("deve lançar DataAlreadyExistException quando usuário já é membro do board")
        void deveLancarDataAlreadyExistException_quandoUsuarioJaEhMembro() {
            MemberInviteRequest request = new MemberInviteRequest(LOGIN);
            when(kanbanBoardService.findById(BOARD_ID)).thenReturn(board);
            when(userService.findByLogin(LOGIN)).thenReturn(user);
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMember(InvitedStatus.PENDING, board)));

            assertThatThrownBy(() -> kanbanMemberService.inviteMember(BOARD_ID, request))
                    .isInstanceOf(DataAlreadyExistException.class)
                    .hasMessageContaining(LOGIN);

            verify(kanbanMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve propagar DataNotFoundException quando board não existe")
        void devePropagarDataNotFoundException_quandoBoardNaoExiste() {
            MemberInviteRequest request = new MemberInviteRequest(LOGIN);
            when(kanbanBoardService.findById(BOARD_ID))
                    .thenThrow(new DataNotFoundException("Kanban Board with id " + BOARD_ID + " not found"));

            assertThatThrownBy(() -> kanbanMemberService.inviteMember(BOARD_ID, request))
                    .isInstanceOf(DataNotFoundException.class);

            verifyNoInteractions(userService, kanbanMemberRepository);
        }

        @Test
        @DisplayName("deve propagar EntityNotFoundException quando login não existe")
        void devePropagarEntityNotFoundException_quandoLoginNaoExiste() {
            MemberInviteRequest request = new MemberInviteRequest(LOGIN);
            when(kanbanBoardService.findById(BOARD_ID)).thenReturn(board);
            when(userService.findByLogin(LOGIN))
                    .thenThrow(new EntityNotFoundException("User not found: " + LOGIN));

            assertThatThrownBy(() -> kanbanMemberService.inviteMember(BOARD_ID, request))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(kanbanMemberRepository);
        }
    }

    @Nested
    @DisplayName("getMembersByBoard")
    class GetMembersByBoard {

        @Test
        @DisplayName("deve retornar lista de membros mapeados quando board existe")
        void deveRetornarListaDeMembrosMapeados_quandoBoardExiste() {
            KanbanMember accepted = buildMember(InvitedStatus.ACCEPTED, board);
            KanbanMember pending = buildMember(InvitedStatus.PENDING, board);
            pending.setKanbanMemberId(11L);
            when(kanbanBoardService.findById(BOARD_ID)).thenReturn(board);
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardId(BOARD_ID))
                    .thenReturn(List.of(accepted, pending));

            List<KanbanMemberResponse> result = kanbanMemberService.getMembersByBoard(BOARD_ID);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(KanbanMemberResponse::invitedStatus)
                    .containsExactly(InvitedStatus.ACCEPTED, InvitedStatus.PENDING);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando board não tem membros")
        void deveRetornarListaVazia_quandoBoardNaoTemMembros() {
            when(kanbanBoardService.findById(BOARD_ID)).thenReturn(board);
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardId(BOARD_ID))
                    .thenReturn(List.of());

            List<KanbanMemberResponse> result = kanbanMemberService.getMembersByBoard(BOARD_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("deve propagar DataNotFoundException quando board não existe")
        void devePropagarDataNotFoundException_quandoBoardNaoExiste() {
            when(kanbanBoardService.findById(BOARD_ID))
                    .thenThrow(new DataNotFoundException("Board not found"));

            assertThatThrownBy(() -> kanbanMemberService.getMembersByBoard(BOARD_ID))
                    .isInstanceOf(DataNotFoundException.class);

            verifyNoInteractions(kanbanMemberRepository);
        }
    }

    @Nested
    @DisplayName("updateMemberStatus")
    class UpdateMemberStatus {

        @Test
        @DisplayName("deve atualizar status para ACCEPTED setando joinedAt")
        void deveAtualizarStatusParaAccepted_setandoJoinedAt() {
            KanbanMember member = buildMember(InvitedStatus.PENDING, board);
            member.setJoinedAt(null);
            when(kanbanMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
            when(kanbanMemberRepository.save(member)).thenReturn(member);

            kanbanMemberService.updateMemberStatus(MEMBER_ID, "ACCEPTED");

            assertThat(member.getInvitedStatus()).isEqualTo(InvitedStatus.ACCEPTED);
            assertThat(member.getJoinedAt()).isNotNull();
        }

        @Test
        @DisplayName("deve atualizar status para DECLINED sem alterar joinedAt")
        void deveAtualizarStatusParaDeclined_semAlterarJoinedAt() {
            KanbanMember member = buildMember(InvitedStatus.PENDING, board);
            member.setJoinedAt(null);
            when(kanbanMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
            when(kanbanMemberRepository.save(member)).thenReturn(member);

            kanbanMemberService.updateMemberStatus(MEMBER_ID, "DECLINED");

            assertThat(member.getInvitedStatus()).isEqualTo(InvitedStatus.DECLINED);
            assertThat(member.getJoinedAt()).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"accepted", "ACCEPTED", "Accepted"})
        @DisplayName("deve aceitar status independentemente do case")
        void deveAceitarStatus_independenteDoCase(String statusInput) {
            KanbanMember member = buildMember(InvitedStatus.PENDING, board);
            member.setJoinedAt(null);
            when(kanbanMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
            when(kanbanMemberRepository.save(member)).thenReturn(member);

            kanbanMemberService.updateMemberStatus(MEMBER_ID, statusInput);

            assertThat(member.getInvitedStatus()).isEqualTo(InvitedStatus.ACCEPTED);
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException quando status é inválido")
        void deveLancarIllegalArgumentException_quandoStatusInvalido() {
            assertThatThrownBy(() -> kanbanMemberService.updateMemberStatus(MEMBER_ID, "INVALID"))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(kanbanMemberRepository);
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando member não existe")
        void deveLancarDataNotFoundException_quandoMemberNaoExiste() {
            when(kanbanMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanMemberService.updateMemberStatus(MEMBER_ID, "ACCEPTED"))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessageContaining("Member with id " + MEMBER_ID);

            verify(kanbanMemberRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeMember")
    class RemoveMember {

        @Test
        @DisplayName("deve deletar member quando pertence ao board")
        void deveDeletarMember_quandoPertenceAoBoard() {
            KanbanMember member = buildMember(InvitedStatus.ACCEPTED, board);
            when(kanbanMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

            kanbanMemberService.removeMember(BOARD_ID, MEMBER_ID);

            verify(kanbanMemberRepository).delete(member);
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando member pertence a outro board")
        void deveLancarDataNotFoundException_quandoMemberPertenceAOutroBoard() {
            KanbanBoard otherBoard = KanbanBoard.builder().kanbanBoardId(999L).build();
            KanbanMember member = buildMember(InvitedStatus.ACCEPTED, otherBoard);
            when(kanbanMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

            assertThatThrownBy(() -> kanbanMemberService.removeMember(BOARD_ID, MEMBER_ID))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessageContaining("not found in board " + BOARD_ID);

            verify(kanbanMemberRepository, never()).delete(any(KanbanMember.class));
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando member não existe")
        void deveLancarDataNotFoundException_quandoMemberNaoExiste() {
            when(kanbanMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanMemberService.removeMember(BOARD_ID, MEMBER_ID))
                    .isInstanceOf(DataNotFoundException.class);

            verify(kanbanMemberRepository, never()).delete(any(KanbanMember.class));
        }
    }
}
