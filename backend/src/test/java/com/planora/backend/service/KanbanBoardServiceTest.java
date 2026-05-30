package com.planora.backend.service;

import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.exception.UnauthorizedException;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.model.issue.dto.IssueUpdateRequest;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.kanban.dto.InvitedStatus;
import com.planora.backend.model.kanban.dto.KanbanBoardRequest;
import com.planora.backend.model.kanban.dto.KanbanBoardResponse;
import com.planora.backend.model.kanban.dto.KanbanColumnRequest;
import com.planora.backend.model.kanban.dto.KanbanColumnResponse;
import com.planora.backend.model.kanban.dto.KanbanColumnWithIssuesResponse;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.KanbanBoardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KanbanBoardService")
class KanbanBoardServiceTest {

    private static final String GITHUB_TOKEN = "github-pat-123";
    private static final String OWNER = "llucascr";
    private static final String REPO = "planora";
    private static final String OWNER_REPO = OWNER + "/" + REPO;
    private static final Long USER_ID = 42L;
    private static final Long BOARD_ID = 1L;
    private static final Long COLUMN_ID = 10L;
    private static final Long OTHER_COLUMN_ID = 11L;
    private static final Long ISSUE_ID = 50L;
    private static final Long WEBHOOK_ID = 555L;

    @Mock private KanbanBoardRepository kanbanBoardRepository;
    @Mock private UserService userService;
    @Mock private GithubService githubService;
    @Mock private KanbanWebhookService kanbanWebhookService;
    @Mock private IKanbanColumnService kanbanColumnService;
    @Mock private KanbanIssueService kanbanIssueService;
    @Mock private Jwt jwt;

    @InjectMocks private KanbanBoardService kanbanBoardService;

    private User owner;
    private KanbanBoard board;
    private KanbanColumn column;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .userId(USER_ID)
                .login(OWNER)
                .githubToken(GITHUB_TOKEN)
                .build();
        column = KanbanColumn.builder()
                .kanbanColumnId(COLUMN_ID)
                .name("Todo")
                .position(0)
                .issues(new ArrayList<>())
                .build();
        board = KanbanBoard.builder()
                .kanbanBoardId(BOARD_ID)
                .name("Planora Board")
                .description("desc")
                .githubOwnerName(OWNER)
                .githubRepository(REPO)
                .owner(owner)
                .members(new ArrayList<>())
                .columns(new ArrayList<>(List.of(column)))
                .createdAt(LocalDateTime.now())
                .build();
        column.setKanbanBoard(board);
    }

    @Nested
    @DisplayName("createKanbanBoard")
    class CreateKanbanBoard {

        @Test
        @DisplayName("deve criar board com owner como member ACCEPTED e delegar colunas e webhook")
        void deveCriarBoardComOwnerMemberEDelegarServicos() {
            KanbanBoardRequest request = new KanbanBoardRequest("Novo Board", "Descrição", OWNER_REPO, List.of());
            when(userService.findById(USER_ID)).thenReturn(owner);
            when(githubService.checkIfRepositoryAndOwnerNameAreValid(GITHUB_TOKEN, OWNER, REPO)).thenReturn(true);
            when(kanbanBoardRepository.save(any(KanbanBoard.class))).thenAnswer(invocation -> {
                KanbanBoard b = invocation.getArgument(0);
                b.setKanbanBoardId(BOARD_ID);
                return b;
            });
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            KanbanBoardResponse response = kanbanBoardService.createKanbanBoard(request, USER_ID, GITHUB_TOKEN);

            ArgumentCaptor<KanbanBoard> boardCaptor = ArgumentCaptor.forClass(KanbanBoard.class);
            verify(kanbanBoardRepository).save(boardCaptor.capture());
            KanbanBoard saved = boardCaptor.getValue();
            assertThat(saved.getName()).isEqualTo("Novo Board");
            assertThat(saved.getDescription()).isEqualTo("Descrição");
            assertThat(saved.getGithubOwnerName()).isEqualTo(OWNER);
            assertThat(saved.getGithubRepository()).isEqualTo(REPO);
            assertThat(saved.getOwner()).isSameAs(owner);
            assertThat(saved.getMembers()).hasSize(1);
            assertThat(saved.getMembers().get(0).getInvitedStatus()).isEqualTo(InvitedStatus.ACCEPTED);
            assertThat(saved.getMembers().get(0).getUser()).isSameAs(owner);

            verify(kanbanColumnService).createDefaultColumns(any(KanbanBoard.class));
            verify(kanbanWebhookService).registerWebhookIfNeeded(any(KanbanBoard.class), eq(GITHUB_TOKEN), eq(OWNER), eq(REPO));
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException quando githubRepository não tem 2 partes")
        void deveLancarIllegalArgumentException_quandoFormatoInvalido() {
            KanbanBoardRequest request = new KanbanBoardRequest("Board", "d", "sem-barra", List.of());
            when(userService.findById(USER_ID)).thenReturn(owner);

            assertThatThrownBy(() -> kanbanBoardService.createKanbanBoard(request, USER_ID, GITHUB_TOKEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("owner/repository");

            verifyNoInteractions(githubService);
            verify(kanbanBoardRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando repositório inválido no GitHub")
        void deveLancarDataNotFoundException_quandoRepositorioInvalido() {
            KanbanBoardRequest request = new KanbanBoardRequest("Board", "d", OWNER_REPO, List.of());
            when(userService.findById(USER_ID)).thenReturn(owner);
            when(githubService.checkIfRepositoryAndOwnerNameAreValid(GITHUB_TOKEN, OWNER, REPO)).thenReturn(false);

            assertThatThrownBy(() -> kanbanBoardService.createKanbanBoard(request, USER_ID, GITHUB_TOKEN))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessageContaining(OWNER_REPO);

            verify(kanbanBoardRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("deve retornar board quando existe")
        void deveRetornar_quandoExiste() {
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            assertThat(kanbanBoardService.findById(BOARD_ID)).isSameAs(board);
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando board não existe")
        void deveLancar_quandoNaoExiste() {
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanBoardService.findById(BOARD_ID))
                    .isInstanceOf(DataNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAllBoardsByUser")
    class GetAllBoardsByUser {

        @Test
        @DisplayName("deve retornar boards mapeados para resposta")
        void deveRetornarBoardsMapeados() {
            when(kanbanBoardRepository.findBoardsByMemberUserIdAndStatus(USER_ID, InvitedStatus.ACCEPTED))
                    .thenReturn(List.of(board));

            List<KanbanBoardResponse> result = kanbanBoardService.getAllBoardsByUser(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).kanbanBoardId()).isEqualTo(BOARD_ID);
            assertThat(result.get(0).ownerLogin()).isEqualTo(OWNER);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando usuário não tem boards ACCEPTED")
        void deveRetornarListaVazia() {
            when(kanbanBoardRepository.findBoardsByMemberUserIdAndStatus(USER_ID, InvitedStatus.ACCEPTED))
                    .thenReturn(List.of());

            assertThat(kanbanBoardService.getAllBoardsByUser(USER_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBoardById")
    class GetBoardById {

        @Test
        @DisplayName("deve retornar resposta mapeada quando board existe")
        void deveRetornarRespostaMapeada() {
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            KanbanBoardResponse response = kanbanBoardService.getBoardById(BOARD_ID);

            assertThat(response.kanbanBoardId()).isEqualTo(BOARD_ID);
            assertThat(response.githubOwnerName()).isEqualTo(OWNER);
            assertThat(response.webhookActive()).isFalse();
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando board não existe")
        void deveLancarDataNotFound_quandoNaoExiste() {
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanBoardService.getBoardById(BOARD_ID))
                    .isInstanceOf(DataNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateBoard")
    class UpdateBoard {

        @Test
        @DisplayName("deve atualizar name e description e persistir")
        void deveAtualizarNameEDescription() {
            KanbanBoardRequest request = new KanbanBoardRequest("Novo nome", "Nova descrição", OWNER_REPO, List.of());
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(kanbanBoardRepository.save(board)).thenReturn(board);

            KanbanBoardResponse response = kanbanBoardService.updateBoard(BOARD_ID, request);

            assertThat(board.getName()).isEqualTo("Novo nome");
            assertThat(board.getDescription()).isEqualTo("Nova descrição");
            assertThat(response.name()).isEqualTo("Novo nome");
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando board não existe")
        void deveLancarDataNotFound_quandoNaoExiste() {
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.empty());
            KanbanBoardRequest request = new KanbanBoardRequest("n", "d", OWNER_REPO, List.of());

            assertThatThrownBy(() -> kanbanBoardService.updateBoard(BOARD_ID, request))
                    .isInstanceOf(DataNotFoundException.class);

            verify(kanbanBoardRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteBoard")
    class DeleteBoard {

        @Test
        @DisplayName("deve delegar remoção de webhook ao KanbanWebhookService e deletar board")
        void deveDelegarWebhookEDeletar() {
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            kanbanBoardService.deleteBoard(BOARD_ID);

            verify(kanbanWebhookService).removeWebhookIfLastBoard(board);
            verify(kanbanBoardRepository).delete(board);
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando board não existe")
        void deveLancarDataNotFound_quandoBoardNaoExiste() {
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanBoardService.deleteBoard(BOARD_ID))
                    .isInstanceOf(DataNotFoundException.class);

            verify(kanbanBoardRepository, never()).delete(any(KanbanBoard.class));
        }
    }

    @Nested
    @DisplayName("registerWebhook")
    class RegisterWebhook {

        @Test
        @DisplayName("deve delegar para KanbanWebhookService e mapear resposta")
        void deveDelegarEMapearResposta() {
            board.setGithubWebhookId(WEBHOOK_ID);
            when(kanbanWebhookService.registerWebhook(BOARD_ID, GITHUB_TOKEN)).thenReturn(board);

            KanbanBoardResponse response = kanbanBoardService.registerWebhook(BOARD_ID, GITHUB_TOKEN);

            verify(kanbanWebhookService).registerWebhook(BOARD_ID, GITHUB_TOKEN);
            assertThat(response.webhookActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("createColumn")
    class CreateColumn {

        @Test
        @DisplayName("deve delegar criação para KanbanColumnService e propagar resposta")
        void deveDelegarEPropagarResposta() {
            KanbanColumnRequest request = new KanbanColumnRequest(null, "Nova", null);
            KanbanColumnResponse expected = new KanbanColumnResponse(COLUMN_ID, "Nova", 0);
            when(kanbanColumnService.createColumn(BOARD_ID, request)).thenReturn(expected);

            KanbanColumnResponse response = kanbanBoardService.createColumn(BOARD_ID, request);

            assertThat(response).isSameAs(expected);
            verify(kanbanColumnService).createColumn(BOARD_ID, request);
        }
    }

    @Nested
    @DisplayName("updateColumn")
    class UpdateColumn {

        @Test
        @DisplayName("deve delegar atualização para KanbanColumnService e propagar resposta")
        void deveDelegarEPropagarResposta() {
            KanbanColumnResponse expected = new KanbanColumnResponse(COLUMN_ID, "Nova", 0);
            when(kanbanColumnService.updateColumn(BOARD_ID, COLUMN_ID, "Nova", null, USER_ID)).thenReturn(expected);

            KanbanColumnResponse response = kanbanBoardService.updateColumn(BOARD_ID, COLUMN_ID, "Nova", null, USER_ID);

            assertThat(response).isSameAs(expected);
            verify(kanbanColumnService).updateColumn(BOARD_ID, COLUMN_ID, "Nova", null, USER_ID);
        }

        @Test
        @DisplayName("deve propagar UnauthorizedException do KanbanColumnService")
        void devePropagar_quandoNaoEhMember() {
            when(kanbanColumnService.updateColumn(BOARD_ID, COLUMN_ID, "n", null, USER_ID))
                    .thenThrow(new UnauthorizedException("Kanban member not found"));

            assertThatThrownBy(() -> kanbanBoardService.updateColumn(BOARD_ID, COLUMN_ID, "n", null, USER_ID))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("deleteColumn")
    class DeleteColumn {

        @Test
        @DisplayName("deve delegar deleção para KanbanColumnService")
        void deveDelegarDelecao() {
            kanbanBoardService.deleteColumn(jwt, BOARD_ID, COLUMN_ID, USER_ID);

            verify(kanbanColumnService).deleteColumn(jwt, BOARD_ID, COLUMN_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("getColumns")
    class GetColumns {

        @Test
        @DisplayName("deve delegar para KanbanColumnService e propagar lista")
        void deveDelegarEPropagarLista() {
            List<KanbanColumnResponse> expected = List.of(new KanbanColumnResponse(COLUMN_ID, "Todo", 0));
            when(kanbanColumnService.getColumns(BOARD_ID, USER_ID)).thenReturn(expected);

            List<KanbanColumnResponse> result = kanbanBoardService.getColumns(BOARD_ID, USER_ID);

            assertThat(result).isSameAs(expected);
            verify(kanbanColumnService).getColumns(BOARD_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("getColumnsWithIssues")
    class GetColumnsWithIssues {

        @Test
        @DisplayName("deve delegar para KanbanColumnService e propagar resposta")
        void deveDelegarEPropagarResposta() {
            List<KanbanColumnWithIssuesResponse> expected = List.of();
            when(kanbanColumnService.getColumnsWithIssues(BOARD_ID, USER_ID)).thenReturn(expected);

            List<KanbanColumnWithIssuesResponse> result = kanbanBoardService.getColumnsWithIssues(BOARD_ID, USER_ID);

            assertThat(result).isSameAs(expected);
            verify(kanbanColumnService).getColumnsWithIssues(BOARD_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("createIssueAndAddToColumn")
    class CreateIssueAndAddToColumn {

        @Test
        @DisplayName("deve delegar para KanbanIssueService e propagar retorno")
        void deveDelegarEPropagarRetorno() {
            IssueRequest request = new IssueRequest("t", "b", List.of(), List.of());
            IssueResponse expected = mockIssueResponse();
            when(kanbanIssueService.createIssueAndAddToColumn(BOARD_ID, COLUMN_ID, jwt, request, USER_ID, REPO))
                    .thenReturn(expected);

            IssueResponse response = kanbanBoardService.createIssueAndAddToColumn(
                    BOARD_ID, COLUMN_ID, jwt, request, USER_ID, REPO);

            assertThat(response).isSameAs(expected);
        }

        @Test
        @DisplayName("deve propagar UnauthorizedException do KanbanIssueService")
        void devePropagar_quandoNaoEhMember() {
            IssueRequest request = new IssueRequest("t", "b", List.of(), List.of());
            when(kanbanIssueService.createIssueAndAddToColumn(BOARD_ID, COLUMN_ID, jwt, request, USER_ID, REPO))
                    .thenThrow(new UnauthorizedException("Kanban member not found"));

            assertThatThrownBy(() -> kanbanBoardService.createIssueAndAddToColumn(
                    BOARD_ID, COLUMN_ID, jwt, request, USER_ID, REPO))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("createBulkIssuesAndAddToColumn")
    class CreateBulkIssuesAndAddToColumn {

        @Test
        @DisplayName("deve delegar para KanbanIssueService e propagar retorno")
        void deveDelegarEPropagarRetorno() {
            List<IssueRequest> requests = List.of(new IssueRequest("t", "b", List.of(), List.of()));
            List<IssueResponse> expected = List.of(mockIssueResponse());
            when(kanbanIssueService.createBulkIssuesAndAddToColumn(BOARD_ID, COLUMN_ID, jwt, requests, USER_ID, REPO))
                    .thenReturn(expected);

            List<IssueResponse> response = kanbanBoardService.createBulkIssuesAndAddToColumn(
                    BOARD_ID, COLUMN_ID, jwt, requests, USER_ID, REPO);

            assertThat(response).isSameAs(expected);
        }
    }

    @Nested
    @DisplayName("delegators de Issue (openIssue / closeIssue / updateIssue / deleteIssue)")
    class IssueDelegators {

        @Test
        @DisplayName("openIssue deve delegar para KanbanIssueService e propagar retorno")
        void openIssueDeveDelegar() {
            IssueResponse expected = mockIssueResponse();
            when(kanbanIssueService.openIssue(jwt, ISSUE_ID)).thenReturn(expected);

            assertThat(kanbanBoardService.openIssue(jwt, ISSUE_ID)).isSameAs(expected);
            verify(kanbanIssueService).openIssue(jwt, ISSUE_ID);
        }

        @Test
        @DisplayName("closeIssue deve delegar para KanbanIssueService e propagar retorno")
        void closeIssueDeveDelegar() {
            IssueResponse expected = mockIssueResponse();
            when(kanbanIssueService.closeIssue(jwt, ISSUE_ID)).thenReturn(expected);

            assertThat(kanbanBoardService.closeIssue(jwt, ISSUE_ID)).isSameAs(expected);
            verify(kanbanIssueService).closeIssue(jwt, ISSUE_ID);
        }

        @Test
        @DisplayName("updateIssue deve delegar para KanbanIssueService e propagar retorno")
        void updateIssueDeveDelegar() {
            IssueUpdateRequest request = new IssueUpdateRequest("t", "b", "open", null, null);
            IssueResponse expected = mockIssueResponse();
            when(kanbanIssueService.updateIssue(jwt, ISSUE_ID, request)).thenReturn(expected);

            assertThat(kanbanBoardService.updateIssue(jwt, ISSUE_ID, request)).isSameAs(expected);
            verify(kanbanIssueService).updateIssue(jwt, ISSUE_ID, request);
        }

        @Test
        @DisplayName("deleteIssue deve delegar para KanbanIssueService")
        void deleteIssueDeveDelegar() {
            kanbanBoardService.deleteIssue(jwt, ISSUE_ID);

            verify(kanbanIssueService).deleteIssue(jwt, ISSUE_ID);
        }
    }

    @Nested
    @DisplayName("moveIssue")
    class MoveIssue {

        @Test
        @DisplayName("deve delegar para KanbanIssueService")
        void deveDelegarParaIssueService() {
            kanbanBoardService.moveIssue(BOARD_ID, ISSUE_ID, OTHER_COLUMN_ID, USER_ID);

            verify(kanbanIssueService).moveIssue(BOARD_ID, ISSUE_ID, OTHER_COLUMN_ID, USER_ID);
        }

        @Test
        @DisplayName("deve propagar UnauthorizedException do KanbanIssueService")
        void devePropagar_quandoNaoEhMember() {
            doThrow(new UnauthorizedException("Kanban member not found"))
                    .when(kanbanIssueService).moveIssue(BOARD_ID, ISSUE_ID, OTHER_COLUMN_ID, USER_ID);

            assertThatThrownBy(() -> kanbanBoardService.moveIssue(BOARD_ID, ISSUE_ID, OTHER_COLUMN_ID, USER_ID))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    private IssueResponse mockIssueResponse() {
        return new IssueResponse(null, null, null, null);
    }
}
