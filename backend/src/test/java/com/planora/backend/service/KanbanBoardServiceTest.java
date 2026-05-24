package com.planora.backend.service;

import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.exception.UnauthorizedException;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.State;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.model.issue.dto.IssueUpdateRequest;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.kanban.KanbanMember;
import com.planora.backend.model.kanban.dto.InvitedStatus;
import com.planora.backend.model.kanban.dto.KanbanBoardRequest;
import com.planora.backend.model.kanban.dto.KanbanBoardResponse;
import com.planora.backend.model.kanban.dto.KanbanColumnRequest;
import com.planora.backend.model.kanban.dto.KanbanColumnResponse;
import com.planora.backend.model.kanban.dto.KanbanColumnWithIssuesResponse;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.KanbanBoardRepository;
import com.planora.backend.repository.KanbanColumnRepository;
import com.planora.backend.repository.KanbanMemberRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
    @Mock private KanbanColumnRepository kanbanColumnRepository;
    @Mock private KanbanMemberRepository kanbanMemberRepository;
    @Mock private UserService userService;
    @Mock private GithubService githubService;
    @Mock private Jwt jwt;

    @InjectMocks private KanbanBoardService kanbanBoardService;

    private User owner;
    private KanbanBoard board;
    private KanbanColumn column;
    private KanbanColumn otherColumn;

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
        otherColumn = KanbanColumn.builder()
                .kanbanColumnId(OTHER_COLUMN_ID)
                .name("Done")
                .position(1)
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
                .columns(new ArrayList<>(List.of(column, otherColumn)))
                .createdAt(LocalDateTime.now())
                .build();
        column.setKanbanBoard(board);
        otherColumn.setKanbanBoard(board);
    }

    private KanbanMember buildMembership() {
        return KanbanMember.builder()
                .kanbanMemberId(100L)
                .kanbanBoard(board)
                .user(owner)
                .invitedStatus(InvitedStatus.ACCEPTED)
                .invitedAt(LocalDateTime.now())
                .joinedAt(LocalDateTime.now())
                .build();
    }

    private Issue buildIssue(Long id, KanbanColumn col) {
        Issue issue = new Issue();
        issue.setIssueId(id);
        issue.setNumber(99);
        issue.setTitle("Issue " + id);
        issue.setState(State.OPEN);
        issue.setColumn(col);
        col.getIssues().add(issue);
        return issue;
    }

    @Nested
    @DisplayName("createKanbanBoard")
    class CreateKanbanBoard {

        @Test
        @DisplayName("deve criar board com colunas padrão, owner como member ACCEPTED e webhook registrado")
        void deveCriarBoardComColunasPadraoOwnerMemberEWebhook() {
            KanbanBoardRequest request = new KanbanBoardRequest("Novo Board", "Descrição", OWNER_REPO, List.of());
            when(userService.findById(USER_ID)).thenReturn(owner);
            when(githubService.checkIfRepositoryAndOwnerNameAreValid(GITHUB_TOKEN, OWNER, REPO)).thenReturn(true);
            when(kanbanBoardRepository.save(any(KanbanBoard.class))).thenAnswer(invocation -> {
                KanbanBoard b = invocation.getArgument(0);
                b.setKanbanBoardId(BOARD_ID);
                return b;
            });
            when(kanbanBoardRepository.findBoardsByOwnerAndRepositoryWithWebhook(OWNER, REPO))
                    .thenReturn(List.of());
            when(githubService.createRepositoryWebhook(GITHUB_TOKEN, OWNER, REPO)).thenReturn(WEBHOOK_ID);
            when(kanbanBoardRepository.findById(BOARD_ID)).thenAnswer(invocation -> Optional.of(board));

            KanbanBoardResponse response = kanbanBoardService.createKanbanBoard(request, USER_ID, GITHUB_TOKEN);

            ArgumentCaptor<KanbanBoard> boardCaptor = ArgumentCaptor.forClass(KanbanBoard.class);
            verify(kanbanBoardRepository, org.mockito.Mockito.atLeastOnce()).save(boardCaptor.capture());
            KanbanBoard saved = boardCaptor.getAllValues().get(0);
            assertThat(saved.getName()).isEqualTo("Novo Board");
            assertThat(saved.getDescription()).isEqualTo("Descrição");
            assertThat(saved.getGithubOwnerName()).isEqualTo(OWNER);
            assertThat(saved.getGithubRepository()).isEqualTo(REPO);
            assertThat(saved.getOwner()).isSameAs(owner);
            assertThat(saved.getMembers()).hasSize(1);
            assertThat(saved.getMembers().get(0).getInvitedStatus()).isEqualTo(InvitedStatus.ACCEPTED);
            assertThat(saved.getMembers().get(0).getUser()).isSameAs(owner);

            ArgumentCaptor<List<KanbanColumn>> columnsCaptor = ArgumentCaptor.forClass(List.class);
            verify(kanbanColumnRepository).saveAll(columnsCaptor.capture());
            List<KanbanColumn> defaultColumns = columnsCaptor.getValue();
            assertThat(defaultColumns).hasSize(3);
            assertThat(defaultColumns).extracting(KanbanColumn::getName)
                    .containsExactly("Todo", "In Progress", "Done");
            assertThat(defaultColumns).extracting(KanbanColumn::getPosition)
                    .containsExactly(0, 1, 2);

            KanbanBoard latestSaved = boardCaptor.getAllValues().get(boardCaptor.getAllValues().size() - 1);
            assertThat(latestSaved.getGithubWebhookId()).isEqualTo(WEBHOOK_ID);
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

        @Test
        @DisplayName("deve concluir criação sem propagar erro quando createRepositoryWebhook falha")
        void deveConcluir_quandoCreateRepositoryWebhookFalha() {
            KanbanBoardRequest request = new KanbanBoardRequest("Board", "d", OWNER_REPO, List.of());
            when(userService.findById(USER_ID)).thenReturn(owner);
            when(githubService.checkIfRepositoryAndOwnerNameAreValid(GITHUB_TOKEN, OWNER, REPO)).thenReturn(true);
            when(kanbanBoardRepository.save(any(KanbanBoard.class))).thenAnswer(invocation -> {
                KanbanBoard b = invocation.getArgument(0);
                b.setKanbanBoardId(BOARD_ID);
                return b;
            });
            when(kanbanBoardRepository.findBoardsByOwnerAndRepositoryWithWebhook(OWNER, REPO))
                    .thenReturn(List.of());
            when(githubService.createRepositoryWebhook(GITHUB_TOKEN, OWNER, REPO))
                    .thenThrow(new RuntimeException("rate-limited"));
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            KanbanBoardResponse response = kanbanBoardService.createKanbanBoard(request, USER_ID, GITHUB_TOKEN);

            assertThat(response).isNotNull();
            assertThat(board.getGithubWebhookId()).isNull();
        }

        @Test
        @DisplayName("deve reutilizar webhookId existente quando outro board do mesmo repo já tem webhook")
        void deveReutilizarWebhookId_quandoOutroBoardTemWebhook() {
            KanbanBoardRequest request = new KanbanBoardRequest("Board", "d", OWNER_REPO, List.of());
            KanbanBoard existingWithWebhook = KanbanBoard.builder()
                    .kanbanBoardId(9L).githubWebhookId(WEBHOOK_ID).build();
            when(userService.findById(USER_ID)).thenReturn(owner);
            when(githubService.checkIfRepositoryAndOwnerNameAreValid(GITHUB_TOKEN, OWNER, REPO)).thenReturn(true);
            when(kanbanBoardRepository.save(any(KanbanBoard.class))).thenAnswer(invocation -> {
                KanbanBoard b = invocation.getArgument(0);
                b.setKanbanBoardId(BOARD_ID);
                return b;
            });
            when(kanbanBoardRepository.findBoardsByOwnerAndRepositoryWithWebhook(OWNER, REPO))
                    .thenReturn(List.of(existingWithWebhook));
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            kanbanBoardService.createKanbanBoard(request, USER_ID, GITHUB_TOKEN);

            verify(githubService, never()).createRepositoryWebhook(any(), any(), any());
            ArgumentCaptor<KanbanBoard> captor = ArgumentCaptor.forClass(KanbanBoard.class);
            verify(kanbanBoardRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
            KanbanBoard latest = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertThat(latest.getGithubWebhookId()).isEqualTo(WEBHOOK_ID);
        }
    }

    @Nested
    @DisplayName("createIssueAndAddToColumn")
    class CreateIssueAndAddToColumn {

        @Test
        @DisplayName("deve delegar criação para GithubService quando user é member e column pertence ao board")
        void deveDelegarParaGithubService_quandoUserEhMember() {
            IssueRequest request = new IssueRequest("t", "b", List.of(), List.of());
            IssueResponse expected = mockIssueResponse();
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(githubService.createIssue(jwt, request, USER_ID, REPO, column)).thenReturn(expected);

            IssueResponse response = kanbanBoardService.createIssueAndAddToColumn(
                    BOARD_ID, COLUMN_ID, jwt, request, USER_ID, REPO);

            assertThat(response).isSameAs(expected);
        }

        @Test
        @DisplayName("deve lançar UnauthorizedException quando user não é member do board")
        void deveLancarUnauthorizedException_quandoNaoEhMember() {
            IssueRequest request = new IssueRequest("t", "b", List.of(), List.of());
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanBoardService.createIssueAndAddToColumn(
                    BOARD_ID, COLUMN_ID, jwt, request, USER_ID, REPO))
                    .isInstanceOf(UnauthorizedException.class);

            verifyNoInteractions(githubService);
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando column não pertence ao board")
        void deveLancarDataNotFoundException_quandoColumnNaoPertenceAoBoard() {
            IssueRequest request = new IssueRequest("t", "b", List.of(), List.of());
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            assertThatThrownBy(() -> kanbanBoardService.createIssueAndAddToColumn(
                    BOARD_ID, 999L, jwt, request, USER_ID, REPO))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessageContaining("Column with id 999");

            verifyNoInteractions(githubService);
        }
    }

    @Nested
    @DisplayName("createBulkIssuesAndAddToColumn")
    class CreateBulkIssuesAndAddToColumn {

        @Test
        @DisplayName("deve delegar criação em massa para GithubService quando user é member e column existe")
        void deveDelegarCriacaoEmMassa() {
            List<IssueRequest> requests = List.of(
                    new IssueRequest("t1", "b1", List.of(), List.of()),
                    new IssueRequest("t2", "b2", List.of(), List.of()));
            List<IssueResponse> expected = List.of(mockIssueResponse(), mockIssueResponse());
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(githubService.createBulkIssues(jwt, requests, USER_ID, REPO, column)).thenReturn(expected);

            List<IssueResponse> response = kanbanBoardService.createBulkIssuesAndAddToColumn(
                    BOARD_ID, COLUMN_ID, jwt, requests, USER_ID, REPO);

            assertThat(response).isEqualTo(expected);
        }

        @Test
        @DisplayName("deve lançar UnauthorizedException quando user não é member")
        void deveLancarUnauthorized_quandoUserNaoEhMember() {
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanBoardService.createBulkIssuesAndAddToColumn(
                    BOARD_ID, COLUMN_ID, jwt, List.of(), USER_ID, REPO))
                    .isInstanceOf(UnauthorizedException.class);

            verifyNoInteractions(githubService);
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando column não pertence ao board")
        void deveLancarDataNotFound_quandoColumnNaoPertenceAoBoard() {
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            assertThatThrownBy(() -> kanbanBoardService.createBulkIssuesAndAddToColumn(
                    BOARD_ID, 999L, jwt, List.of(), USER_ID, REPO))
                    .isInstanceOf(DataNotFoundException.class);

            verifyNoInteractions(githubService);
        }
    }

    @Nested
    @DisplayName("delegators de Issue (openIssue / closeIssue / updateIssue / deleteIssue)")
    class IssueDelegators {

        @Test
        @DisplayName("openIssue deve delegar para GithubService.openIssue e propagar retorno")
        void openIssueDeveDelegar() {
            IssueResponse expected = mockIssueResponse();
            when(githubService.openIssue(jwt, ISSUE_ID)).thenReturn(expected);

            IssueResponse response = kanbanBoardService.openIssue(jwt, ISSUE_ID);

            assertThat(response).isSameAs(expected);
            verify(githubService).openIssue(jwt, ISSUE_ID);
        }

        @Test
        @DisplayName("closeIssue deve delegar para GithubService.closeIssue e propagar retorno")
        void closeIssueDeveDelegar() {
            IssueResponse expected = mockIssueResponse();
            when(githubService.closeIssue(jwt, ISSUE_ID)).thenReturn(expected);

            IssueResponse response = kanbanBoardService.closeIssue(jwt, ISSUE_ID);

            assertThat(response).isSameAs(expected);
            verify(githubService).closeIssue(jwt, ISSUE_ID);
        }

        @Test
        @DisplayName("updateIssue deve delegar para GithubService.updateIssue e propagar retorno")
        void updateIssueDeveDelegar() {
            IssueUpdateRequest request = new IssueUpdateRequest("t", "b", "open", null, null);
            IssueResponse expected = mockIssueResponse();
            when(githubService.updateIssue(jwt, ISSUE_ID, request)).thenReturn(expected);

            IssueResponse response = kanbanBoardService.updateIssue(jwt, ISSUE_ID, request);

            assertThat(response).isSameAs(expected);
            verify(githubService).updateIssue(jwt, ISSUE_ID, request);
        }

        @Test
        @DisplayName("deleteIssue deve delegar para GithubService.deleteIssue")
        void deleteIssueDeveDelegar() {
            kanbanBoardService.deleteIssue(jwt, ISSUE_ID);

            verify(githubService).deleteIssue(jwt, ISSUE_ID);
        }
    }

    @Nested
    @DisplayName("getKanbanBoard / findById")
    class GetKanbanBoardAndFindById {

        @Test
        @DisplayName("getKanbanBoard deve retornar board quando existe")
        void getKanbanBoardDeveRetornar_quandoExiste() {
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            assertThat(kanbanBoardService.getKanbanBoard(BOARD_ID)).isSameAs(board);
        }

        @Test
        @DisplayName("getKanbanBoard deve lançar DataNotFoundException quando board não existe")
        void getKanbanBoardDeveLancar_quandoNaoExiste() {
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanBoardService.getKanbanBoard(BOARD_ID))
                    .isInstanceOf(DataNotFoundException.class);
        }

        @Test
        @DisplayName("findById deve retornar board quando existe")
        void findByIdDeveRetornar_quandoExiste() {
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            assertThat(kanbanBoardService.findById(BOARD_ID)).isSameAs(board);
        }

        @Test
        @DisplayName("findById deve lançar DataNotFoundException quando board não existe")
        void findByIdDeveLancar_quandoNaoExiste() {
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
        @DisplayName("deve retornar lista vazia quando usuário não é member de nenhum board ACCEPTED")
        void deveRetornarListaVazia_quandoNenhumBoard() {
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
        @DisplayName("deve deletar webhook do GitHub quando é o último board com mesmo owner/repo")
        void deveDeletarWebhook_quandoUltimoBoard() {
            board.setGithubWebhookId(WEBHOOK_ID);
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(kanbanBoardRepository.findByOwnerAndRepositoryExcluding(OWNER, REPO, BOARD_ID))
                    .thenReturn(List.of());

            kanbanBoardService.deleteBoard(BOARD_ID);

            verify(githubService).deleteRepositoryWebhook(GITHUB_TOKEN, OWNER, REPO, WEBHOOK_ID);
            verify(kanbanBoardRepository).delete(board);
        }

        @Test
        @DisplayName("não deve deletar webhook quando existem outros boards do mesmo repo")
        void naoDeveDeletarWebhook_quandoExistemOutrosBoards() {
            board.setGithubWebhookId(WEBHOOK_ID);
            KanbanBoard other = KanbanBoard.builder().kanbanBoardId(99L).build();
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(kanbanBoardRepository.findByOwnerAndRepositoryExcluding(OWNER, REPO, BOARD_ID))
                    .thenReturn(List.of(other));

            kanbanBoardService.deleteBoard(BOARD_ID);

            verify(githubService, never()).deleteRepositoryWebhook(any(), any(), any(), any());
            verify(kanbanBoardRepository).delete(board);
        }

        @Test
        @DisplayName("deve deletar sem chamar GitHub quando board não tem webhookId")
        void deveDeletarSemChamarGithub_quandoSemWebhook() {
            board.setGithubWebhookId(null);
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            kanbanBoardService.deleteBoard(BOARD_ID);

            verifyNoInteractions(githubService);
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
        @DisplayName("deve retornar board atual sem chamar GitHub quando já tem webhookId")
        void deveRetornarBoardAtual_quandoJaTemWebhook() {
            board.setGithubWebhookId(WEBHOOK_ID);
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            KanbanBoardResponse response = kanbanBoardService.registerWebhook(BOARD_ID, GITHUB_TOKEN);

            assertThat(response.webhookActive()).isTrue();
            verifyNoInteractions(githubService);
            verify(kanbanBoardRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve reutilizar webhookId de outro board do mesmo repo")
        void deveReutilizarWebhookId_quandoOutroBoardJaTem() {
            board.setGithubWebhookId(null);
            KanbanBoard sibling = KanbanBoard.builder().kanbanBoardId(2L).githubWebhookId(WEBHOOK_ID).build();
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(kanbanBoardRepository.findBoardsByOwnerAndRepositoryWithWebhook(OWNER, REPO))
                    .thenReturn(List.of(sibling));

            kanbanBoardService.registerWebhook(BOARD_ID, GITHUB_TOKEN);

            assertThat(board.getGithubWebhookId()).isEqualTo(WEBHOOK_ID);
            verify(githubService, never()).createRepositoryWebhook(any(), any(), any());
            verify(kanbanBoardRepository).save(board);
        }

        @Test
        @DisplayName("deve criar novo webhook via GithubService quando nenhum existe")
        void deveCriarNovoWebhook_quandoNenhumExiste() {
            board.setGithubWebhookId(null);
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(kanbanBoardRepository.findBoardsByOwnerAndRepositoryWithWebhook(OWNER, REPO))
                    .thenReturn(List.of());
            when(githubService.createRepositoryWebhook(GITHUB_TOKEN, OWNER, REPO)).thenReturn(WEBHOOK_ID);

            kanbanBoardService.registerWebhook(BOARD_ID, GITHUB_TOKEN);

            assertThat(board.getGithubWebhookId()).isEqualTo(WEBHOOK_ID);
            verify(kanbanBoardRepository).save(board);
        }
    }

    @Nested
    @DisplayName("createColumn")
    class CreateColumn {

        @Test
        @DisplayName("deve criar column com position = tamanho atual da lista de columns")
        void deveCriarColumnComPosicaoCorreta() {
            KanbanColumnRequest request = new KanbanColumnRequest(null, "Nova", null);
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            when(kanbanColumnRepository.save(any(KanbanColumn.class))).thenAnswer(invocation -> invocation.getArgument(0));

            KanbanColumnResponse response = kanbanBoardService.createColumn(BOARD_ID, request);

            ArgumentCaptor<KanbanColumn> captor = ArgumentCaptor.forClass(KanbanColumn.class);
            verify(kanbanColumnRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Nova");
            assertThat(captor.getValue().getPosition()).isEqualTo(2);
            assertThat(captor.getValue().getKanbanBoard()).isSameAs(board);
            assertThat(response.name()).isEqualTo("Nova");
            assertThat(response.position()).isEqualTo(2);
        }

        @Test
        @DisplayName("deve lançar RuntimeException quando board não existe")
        void deveLancarRuntimeException_quandoBoardNaoExiste() {
            KanbanColumnRequest request = new KanbanColumnRequest(null, "n", null);
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanBoardService.createColumn(BOARD_ID, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Board not found");
        }
    }

    @Nested
    @DisplayName("updateColumn")
    class UpdateColumn {

        @Test
        @DisplayName("deve atualizar apenas name quando position é null")
        void deveAtualizarApenasName_quandoPositionNull() {
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column));

            KanbanColumnResponse response = kanbanBoardService.updateColumn(BOARD_ID, COLUMN_ID, "Renomeada", null, USER_ID);

            assertThat(column.getName()).isEqualTo("Renomeada");
            verify(kanbanColumnRepository).save(column);
            verify(kanbanColumnRepository, never()).saveAll(anyList());
            assertThat(response.name()).isEqualTo("Renomeada");
        }

        @Test
        @DisplayName("deve atualizar position reordenando todas as columns")
        void deveAtualizarPositionReordenando() {
            KanbanColumn c1 = KanbanColumn.builder().kanbanColumnId(COLUMN_ID).name("A").position(0).kanbanBoard(board).build();
            KanbanColumn c2 = KanbanColumn.builder().kanbanColumnId(OTHER_COLUMN_ID).name("B").position(1).kanbanBoard(board).build();
            KanbanColumn c3 = KanbanColumn.builder().kanbanColumnId(12L).name("C").position(2).kanbanBoard(board).build();
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(c1));
            when(kanbanColumnRepository.findByKanbanBoard_KanbanBoardIdOrderByPosition(BOARD_ID))
                    .thenReturn(new ArrayList<>(List.of(c1, c2, c3)));

            kanbanBoardService.updateColumn(BOARD_ID, COLUMN_ID, null, 2, USER_ID);

            ArgumentCaptor<List<KanbanColumn>> captor = ArgumentCaptor.forClass(List.class);
            verify(kanbanColumnRepository).saveAll(captor.capture());
            List<KanbanColumn> saved = captor.getValue();
            assertThat(saved).extracting(KanbanColumn::getKanbanColumnId)
                    .containsExactly(OTHER_COLUMN_ID, 12L, COLUMN_ID);
            assertThat(saved).extracting(KanbanColumn::getPosition).containsExactly(0, 1, 2);
        }

        @Test
        @DisplayName("não deve atualizar name quando é null ou em branco")
        void naoDeveAtualizarName_quandoNullOuBranco() {
            String original = column.getName();
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column));

            kanbanBoardService.updateColumn(BOARD_ID, COLUMN_ID, "   ", null, USER_ID);

            assertThat(column.getName()).isEqualTo(original);
        }

        @Test
        @DisplayName("deve lançar UnauthorizedException quando user não é member")
        void deveLancarUnauthorized_quandoUserNaoEhMember() {
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanBoardService.updateColumn(BOARD_ID, COLUMN_ID, "n", null, USER_ID))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando column não existe")
        void deveLancarDataNotFound_quandoColumnNaoExiste() {
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanBoardService.updateColumn(BOARD_ID, COLUMN_ID, "n", null, USER_ID))
                    .isInstanceOf(DataNotFoundException.class);
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando column pertence a outro board")
        void deveLancarDataNotFound_quandoColumnPertenceAOutroBoard() {
            KanbanBoard outroBoard = KanbanBoard.builder().kanbanBoardId(999L).build();
            KanbanColumn outraColumn = KanbanColumn.builder().kanbanColumnId(COLUMN_ID).kanbanBoard(outroBoard).build();
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(outraColumn));

            assertThatThrownBy(() -> kanbanBoardService.updateColumn(BOARD_ID, COLUMN_ID, "n", null, USER_ID))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    @Nested
    @DisplayName("deleteColumn")
    class DeleteColumn {

        @Test
        @DisplayName("deve deletar column e reindexar positions restantes")
        void deveDeletarEReindexar() {
            KanbanColumn c1 = KanbanColumn.builder().kanbanColumnId(COLUMN_ID).position(0).kanbanBoard(board).build();
            KanbanColumn c2 = KanbanColumn.builder().kanbanColumnId(OTHER_COLUMN_ID).position(2).kanbanBoard(board).build();
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(c1));
            when(kanbanColumnRepository.findByKanbanBoard_KanbanBoardIdOrderByPosition(BOARD_ID))
                    .thenReturn(new ArrayList<>(List.of(c2)));

            kanbanBoardService.deleteColumn(BOARD_ID, COLUMN_ID, USER_ID);

            verify(kanbanColumnRepository).delete(c1);
            ArgumentCaptor<List<KanbanColumn>> captor = ArgumentCaptor.forClass(List.class);
            verify(kanbanColumnRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getPosition()).isEqualTo(0);
        }

        @Test
        @DisplayName("deve lançar UnauthorizedException quando user não é member")
        void deveLancarUnauthorized() {
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanBoardService.deleteColumn(BOARD_ID, COLUMN_ID, USER_ID))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando column não existe")
        void deveLancarDataNotFound_quandoColumnNaoExiste() {
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanBoardService.deleteColumn(BOARD_ID, COLUMN_ID, USER_ID))
                    .isInstanceOf(DataNotFoundException.class);
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando column pertence a outro board")
        void deveLancarDataNotFound_quandoColumnPertenceAOutroBoard() {
            KanbanBoard outro = KanbanBoard.builder().kanbanBoardId(999L).build();
            KanbanColumn outraColumn = KanbanColumn.builder().kanbanColumnId(COLUMN_ID).kanbanBoard(outro).build();
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(outraColumn));

            assertThatThrownBy(() -> kanbanBoardService.deleteColumn(BOARD_ID, COLUMN_ID, USER_ID))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    @Nested
    @DisplayName("getColumns")
    class GetColumns {

        @Test
        @DisplayName("deve retornar columns ordenadas por position")
        void deveRetornarColumnsOrdenadas() {
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanColumnRepository.findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(BOARD_ID))
                    .thenReturn(List.of(column, otherColumn));

            List<KanbanColumnResponse> result = kanbanBoardService.getColumns(BOARD_ID, USER_ID);

            assertThat(result).extracting(KanbanColumnResponse::position).containsExactly(0, 1);
        }

        @Test
        @DisplayName("deve lançar UnauthorizedException quando user não é member")
        void deveLancarUnauthorized() {
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanBoardService.getColumns(BOARD_ID, USER_ID))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("moveIssue")
    class MoveIssue {

        @Test
        @DisplayName("deve mover issue da currentColumn para targetColumn e persistir ambas")
        void deveMoverIssueEntreColumns() {
            Issue issue = buildIssue(ISSUE_ID, column);
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            kanbanBoardService.moveIssue(BOARD_ID, ISSUE_ID, OTHER_COLUMN_ID, USER_ID);

            assertThat(column.getIssues()).doesNotContain(issue);
            assertThat(issue.getColumn()).isSameAs(otherColumn);
            verify(kanbanColumnRepository).save(column);
            verify(kanbanColumnRepository).save(otherColumn);
        }

        @Test
        @DisplayName("deve lançar UnauthorizedException quando user não é member")
        void deveLancarUnauthorized() {
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanBoardService.moveIssue(BOARD_ID, ISSUE_ID, OTHER_COLUMN_ID, USER_ID))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando issue não existe no board")
        void deveLancarDataNotFound_quandoIssueNaoExiste() {
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            assertThatThrownBy(() -> kanbanBoardService.moveIssue(BOARD_ID, 9999L, OTHER_COLUMN_ID, USER_ID))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessageContaining("Issue not found");
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando target column não existe no board")
        void deveLancarDataNotFound_quandoTargetColumnNaoExiste() {
            buildIssue(ISSUE_ID, column);
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));

            assertThatThrownBy(() -> kanbanBoardService.moveIssue(BOARD_ID, ISSUE_ID, 9999L, USER_ID))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessageContaining("Target column not found");
        }
    }

    @Nested
    @DisplayName("getColumnsWithIssues")
    class GetColumnsWithIssues {

        @Test
        @DisplayName("deve retornar columns com seus issues mapeados via IssueSummaryResponse")
        void deveRetornarColumnsComIssues() {
            buildIssue(ISSUE_ID, column);
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.of(buildMembership()));
            when(kanbanColumnRepository.findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(BOARD_ID))
                    .thenReturn(List.of(column, otherColumn));

            List<KanbanColumnWithIssuesResponse> result = kanbanBoardService.getColumnsWithIssues(BOARD_ID, USER_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).issues()).hasSize(1);
            assertThat(result.get(0).issues().get(0).issueId()).isEqualTo(ISSUE_ID);
            assertThat(result.get(1).issues()).isEmpty();
        }

        @Test
        @DisplayName("deve lançar UnauthorizedException quando user não é member")
        void deveLancarUnauthorized() {
            when(kanbanMemberRepository.findByKanbanBoard_KanbanBoardIdAndUser_UserId(BOARD_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> kanbanBoardService.getColumnsWithIssues(BOARD_ID, USER_ID))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    private IssueResponse mockIssueResponse() {
        return new IssueResponse(null, null, null, null);
    }
}
