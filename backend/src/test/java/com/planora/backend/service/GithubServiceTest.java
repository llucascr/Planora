package com.planora.backend.service;

import com.planora.backend.client.GithubIssueClient;
import com.planora.backend.client.GithubLabelClient;
import com.planora.backend.client.GithubRepositoryClient;
import com.planora.backend.client.GithubWebhookClient;
import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.Label;
import com.planora.backend.model.issue.State;
import com.planora.backend.model.issue.dto.GithubWebhookCreateRequest;
import com.planora.backend.model.issue.dto.GithubWebhookResponse;
import com.planora.backend.model.issue.dto.IssueApiResponse;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.IssueResponse;
import com.planora.backend.model.issue.dto.IssueUpdateRequest;
import com.planora.backend.model.issue.dto.LabelResponse;
import com.planora.backend.model.issue.dto.RepositoryResponse;
import com.planora.backend.model.issue.dto.UserIssueResponse;
import com.planora.backend.model.issue.dto.UserRepositoryResponse;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.IssueRepository;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GithubService")
class GithubServiceTest {

    private static final String GITHUB_API_VERSION = "2022-11-28";
    private static final String GITHUB_TOKEN = "github-pat-123";
    private static final String BEARER_GITHUB_TOKEN = "Bearer " + GITHUB_TOKEN;
    private static final String REPOSITORY = "planora";
    private static final String OWNER = "llucascr";
    private static final Long USER_ID = 42L;
    private static final Long ISSUE_ID = 7L;
    private static final Integer ISSUE_NUMBER = 99;

    @Mock private IssueRepository issueRepository;
    @Mock private UserService userService;
    @Mock private LabelService labelService;
    @Mock private GithubIssueClient githubIssueClient;
    @Mock private GithubRepositoryClient githubRepositoryClient;
    @Mock private GithubWebhookClient githubWebhookClient;
    @Mock private GithubLabelClient githubLabelClient;
    @Mock private TokenService tokenService;
    @Mock private Jwt jwt;

    @InjectMocks private GithubService githubService;

    private User user;
    private KanbanBoard board;
    private KanbanColumn column;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(githubService, "appBaseUrl", "https://planora.example.com");
        ReflectionTestUtils.setField(githubService, "webhookSecret", "");

        user = User.builder()
                .userId(USER_ID)
                .login(OWNER)
                .avatarUrl("https://avatars.example.com/u/42")
                .email("user@example.com")
                .notificationEmail("notify@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        board = KanbanBoard.builder()
                .kanbanBoardId(1L)
                .githubOwnerName(OWNER)
                .githubRepository(REPOSITORY)
                .build();

        column = KanbanColumn.builder()
                .kanbanColumnId(10L)
                .kanbanBoard(board)
                .build();

        lenient().when(tokenService.getGithubToken(jwt)).thenReturn(GITHUB_TOKEN);
    }

    private IssueApiResponse buildApiResponse(String state, List<LabelResponse> labels, List<UserIssueResponse> assignees) {
        return new IssueApiResponse(
                "https://api.github.com/repos/" + OWNER + "/" + REPOSITORY + "/issues/" + ISSUE_NUMBER,
                ISSUE_NUMBER,
                "Título da issue",
                "Corpo da issue",
                state,
                user.toIssueResponse(),
                labels,
                assignees
        );
    }

    private Issue buildPersistedIssue(State state) {
        Issue issue = new Issue();
        issue.setIssueId(ISSUE_ID);
        issue.setNumber(ISSUE_NUMBER);
        issue.setTitle("Original");
        issue.setBody("Original body");
        issue.setState(state);
        issue.setCreatedAt(LocalDateTime.now().minusDays(1));
        issue.setUpdatedAt(LocalDateTime.now().minusHours(1));
        issue.setClosedAt(state == State.CLOSED ? LocalDateTime.now().minusHours(1) : null);
        issue.setUser(user);
        issue.setColumn(column);
        issue.setLabels(new ArrayList<>());
        issue.setAssignees(new ArrayList<>());
        return issue;
    }

    @Nested
    @DisplayName("createIssue")
    class CreateIssue {

        @Test
        @DisplayName("deve persistir issue e retornar resposta quando usuário existe")
        void devePersistirIssueERetornarResposta_quandoUsuarioExiste() {
            LabelResponse labelResponse = new LabelResponse("url", "bug", "f00", "Algo de errado");
            UserIssueResponse knownAssignee = new UserIssueResponse("alice", "avatar", "a@x", null);
            UserIssueResponse unknownAssignee = new UserIssueResponse("ghost", "avatar", "g@x", null);
            User assigneeAlice = User.builder().userId(2L).login("alice").build();

            IssueApiResponse apiResponse = buildApiResponse("open", List.of(labelResponse), List.of(knownAssignee, unknownAssignee));
            IssueRequest issueRequest = new IssueRequest("Título", "Corpo", List.of("alice", "ghost"), List.of("bug"));
            Label persistedLabel = new Label();
            persistedLabel.setName("bug");

            when(userService.findById(USER_ID)).thenReturn(user);
            when(githubIssueClient.createIssue(OWNER, REPOSITORY, BEARER_GITHUB_TOKEN, GITHUB_API_VERSION, issueRequest))
                    .thenReturn(apiResponse);
            when(labelService.resolveOrCreateLabel(labelResponse)).thenReturn(persistedLabel);
            when(userService.findOptionalByLogin("alice")).thenReturn(Optional.of(assigneeAlice));
            when(userService.findOptionalByLogin("ghost")).thenReturn(Optional.empty());

            IssueResponse response = githubService.createIssue(jwt, issueRequest, USER_ID, REPOSITORY, column);

            ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);
            verify(issueRepository).save(issueCaptor.capture());
            Issue saved = issueCaptor.getValue();

            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getColumn()).isEqualTo(column);
            assertThat(saved.getLabels()).containsExactly(persistedLabel);
            assertThat(saved.getAssignees()).containsExactly(assigneeAlice);
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getState()).isEqualTo(State.OPEN);

            assertThat(response.issueApiResponse().owner()).isEqualTo(user.toIssueResponse());
            assertThat(response.issueApiResponse().number()).isEqualTo(ISSUE_NUMBER);
            assertThat(response.closedAt()).isNull();
        }

        @Test
        @DisplayName("deve propagar exceção quando UserService não encontra usuário")
        void devePropagarExcecao_quandoUserServiceNaoEncontraUsuario() {
            IssueRequest issueRequest = new IssueRequest("t", "b", List.of(), List.of());
            when(userService.findById(USER_ID)).thenThrow(new EntityNotFoundException("User not found"));

            assertThatThrownBy(() -> githubService.createIssue(jwt, issueRequest, USER_ID, REPOSITORY, column))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(githubIssueClient, githubRepositoryClient, githubWebhookClient, issueRepository, labelService);
        }
    }

    @Nested
    @DisplayName("createBulkIssues")
    class CreateBulkIssues {

        @Test
        @DisplayName("deve criar múltiplas issues na ordem recebida")
        void deveCriarMultiplasIssuesNaOrdemRecebida() {
            IssueRequest r1 = new IssueRequest("t1", "b1", List.of(), List.of());
            IssueRequest r2 = new IssueRequest("t2", "b2", List.of(), List.of());
            IssueRequest r3 = new IssueRequest("t3", "b3", List.of(), List.of());

            when(userService.findById(USER_ID)).thenReturn(user);
            when(githubIssueClient.createIssue(eq(OWNER), eq(REPOSITORY), eq(BEARER_GITHUB_TOKEN), eq(GITHUB_API_VERSION), eq(r1)))
                    .thenReturn(buildOpenApiResponseWithTitle("t1"));
            when(githubIssueClient.createIssue(eq(OWNER), eq(REPOSITORY), eq(BEARER_GITHUB_TOKEN), eq(GITHUB_API_VERSION), eq(r2)))
                    .thenReturn(buildOpenApiResponseWithTitle("t2"));
            when(githubIssueClient.createIssue(eq(OWNER), eq(REPOSITORY), eq(BEARER_GITHUB_TOKEN), eq(GITHUB_API_VERSION), eq(r3)))
                    .thenReturn(buildOpenApiResponseWithTitle("t3"));

            List<IssueResponse> responses = githubService.createBulkIssues(jwt, List.of(r1, r2, r3), USER_ID, REPOSITORY, column);

            assertThat(responses).hasSize(3);
            assertThat(responses).extracting(resp -> resp.issueApiResponse().title())
                    .containsExactly("t1", "t2", "t3");
            verify(issueRepository, times(3)).save(any(Issue.class));
        }

        @Test
        @DisplayName("deve retornar lista vazia quando requests for vazio")
        void deveRetornarListaVazia_quandoRequestsForVazio() {
            when(userService.findById(USER_ID)).thenReturn(user);

            List<IssueResponse> responses = githubService.createBulkIssues(jwt, List.of(), USER_ID, REPOSITORY, column);

            assertThat(responses).isEmpty();
            verifyNoInteractions(githubIssueClient, githubRepositoryClient, githubWebhookClient, issueRepository, labelService);
        }

        private IssueApiResponse buildOpenApiResponseWithTitle(String title) {
            return new IssueApiResponse("url", ISSUE_NUMBER, title, "body", "open",
                    user.toIssueResponse(), List.of(), List.of());
        }
    }

    @Nested
    @DisplayName("openIssue")
    class OpenIssue {

        @Test
        @DisplayName("deve abrir issue e atualizar estado no banco quando issue existe")
        void deveAbrirIssueEAtualizarEstadoNoBanco_quandoIssueExiste() {
            Issue existing = buildPersistedIssue(State.CLOSED);
            IssueApiResponse apiResponse = buildApiResponse("open", List.of(), List.of());

            when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(existing));
            ArgumentCaptor<IssueUpdateRequest> updateCaptor = ArgumentCaptor.forClass(IssueUpdateRequest.class);
            when(githubIssueClient.updateIssue(
                    eq(OWNER), eq(REPOSITORY), eq(ISSUE_NUMBER),
                    eq(BEARER_GITHUB_TOKEN), eq(GITHUB_API_VERSION), updateCaptor.capture()))
                    .thenReturn(apiResponse);

            IssueResponse response = githubService.openIssue(jwt, ISSUE_ID);

            assertThat(updateCaptor.getValue().state()).isEqualTo("open");
            assertThat(updateCaptor.getValue().title()).isNull();
            assertThat(updateCaptor.getValue().body()).isNull();
            assertThat(updateCaptor.getValue().labels()).isNull();
            assertThat(updateCaptor.getValue().assignees()).isNull();

            assertThat(existing.getState()).isEqualTo(State.OPEN);
            assertThat(existing.getClosedAt()).isNull();
            assertThat(existing.getUpdatedAt()).isNotNull();
            verify(issueRepository).save(existing);
            assertThat(response.closedAt()).isNull();
            assertThat(response.issueApiResponse().owner()).isEqualTo(user.toIssueResponse());
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando issue não existe")
        void deveLancarDataNotFoundException_quandoIssueNaoExiste() {
            when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> githubService.openIssue(jwt, ISSUE_ID))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessageContaining("Issue not found");

            verifyNoInteractions(githubIssueClient);
            verify(issueRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("closeIssue")
    class CloseIssue {

        @Test
        @DisplayName("deve fechar issue e atualizar estado no banco quando issue existe")
        void deveFecharIssueEAtualizarEstadoNoBanco_quandoIssueExiste() {
            Issue existing = buildPersistedIssue(State.OPEN);
            IssueApiResponse apiResponse = buildApiResponse("closed", List.of(), List.of());

            when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(existing));
            ArgumentCaptor<IssueUpdateRequest> updateCaptor = ArgumentCaptor.forClass(IssueUpdateRequest.class);
            when(githubIssueClient.updateIssue(
                    eq(OWNER), eq(REPOSITORY), eq(ISSUE_NUMBER),
                    eq(BEARER_GITHUB_TOKEN), eq(GITHUB_API_VERSION), updateCaptor.capture()))
                    .thenReturn(apiResponse);

            IssueResponse response = githubService.closeIssue(jwt, ISSUE_ID);

            assertThat(updateCaptor.getValue().state()).isEqualTo("closed");
            assertThat(existing.getState()).isEqualTo(State.CLOSED);
            assertThat(existing.getClosedAt()).isNotNull();
            assertThat(existing.getUpdatedAt()).isEqualTo(existing.getClosedAt());
            verify(issueRepository).save(existing);
            assertThat(response.closedAt()).isEqualTo(existing.getClosedAt());
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando issue não existe")
        void deveLancarDataNotFoundException_quandoIssueNaoExiste() {
            when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> githubService.closeIssue(jwt, ISSUE_ID))
                    .isInstanceOf(DataNotFoundException.class);

            verifyNoInteractions(githubIssueClient);
            verify(issueRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateIssue")
    class UpdateIssue {

        @Test
        @DisplayName("deve atualizar issue e sincronizar campos da API quando issue existe")
        void deveAtualizarIssueESincronizarCamposDaApi_quandoIssueExiste() {
            Issue existing = buildPersistedIssue(State.OPEN);
            LabelResponse newLabel = new LabelResponse("u", "feature", "0f0", "d");
            UserIssueResponse newAssignee = new UserIssueResponse("alice", "a", "a@x", null);
            User assigneeAlice = User.builder().userId(2L).login("alice").build();
            IssueApiResponse apiResponse = new IssueApiResponse(
                    "url", ISSUE_NUMBER, "Novo título", "Novo corpo", "closed",
                    user.toIssueResponse(), List.of(newLabel), List.of(newAssignee));
            IssueUpdateRequest request = new IssueUpdateRequest("Novo título", "Novo corpo", null, List.of("feature"), List.of("alice"));
            Label persistedLabel = new Label();
            persistedLabel.setName("feature");

            when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(existing));
            when(githubIssueClient.updateIssue(OWNER, REPOSITORY, ISSUE_NUMBER, BEARER_GITHUB_TOKEN, GITHUB_API_VERSION, request))
                    .thenReturn(apiResponse);
            when(labelService.resolveOrCreateLabel(newLabel)).thenReturn(persistedLabel);
            when(userService.findOptionalByLogin("alice")).thenReturn(Optional.of(assigneeAlice));

            IssueResponse response = githubService.updateIssue(jwt, ISSUE_ID, request);

            assertThat(existing.getTitle()).isEqualTo("Novo título");
            assertThat(existing.getBody()).isEqualTo("Novo corpo");
            assertThat(existing.getLabels()).containsExactly(persistedLabel);
            assertThat(existing.getAssignees()).containsExactly(assigneeAlice);
            assertThat(existing.getState()).isEqualTo(State.CLOSED);
            assertThat(existing.getClosedAt()).isNotNull();
            verify(issueRepository).save(existing);
            assertThat(response.closedAt()).isEqualTo(existing.getClosedAt());
        }

        @Test
        @DisplayName("deve limpar closedAt quando estado volta para OPEN")
        void deveLimparClosedAt_quandoEstadoVoltaParaOpen() {
            Issue existing = buildPersistedIssue(State.CLOSED);
            IssueApiResponse apiResponse = new IssueApiResponse(
                    "url", ISSUE_NUMBER, "t", "b", "open",
                    user.toIssueResponse(), List.of(), List.of());
            IssueUpdateRequest request = new IssueUpdateRequest(null, null, "open", null, null);

            when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(existing));
            when(githubIssueClient.updateIssue(OWNER, REPOSITORY, ISSUE_NUMBER, BEARER_GITHUB_TOKEN, GITHUB_API_VERSION, request))
                    .thenReturn(apiResponse);

            githubService.updateIssue(jwt, ISSUE_ID, request);

            assertThat(existing.getState()).isEqualTo(State.OPEN);
            assertThat(existing.getClosedAt()).isNull();
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando issue não existe")
        void deveLancarDataNotFoundException_quandoIssueNaoExiste() {
            IssueUpdateRequest request = new IssueUpdateRequest(null, null, null, null, null);
            when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> githubService.updateIssue(jwt, ISSUE_ID, request))
                    .isInstanceOf(DataNotFoundException.class);

            verifyNoInteractions(githubIssueClient);
            verify(issueRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteIssue")
    class DeleteIssue {

        @Test
        @DisplayName("deve fechar no GitHub e deletar do banco quando issue está aberta")
        void deveFecharNoGithubEDeletarDoBanco_quandoIssueEstaAberta() {
            Issue existing = buildPersistedIssue(State.OPEN);
            when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(existing));

            ArgumentCaptor<IssueUpdateRequest> updateCaptor = ArgumentCaptor.forClass(IssueUpdateRequest.class);
            when(githubIssueClient.updateIssue(
                    eq(OWNER), eq(REPOSITORY), eq(ISSUE_NUMBER),
                    eq(BEARER_GITHUB_TOKEN), eq(GITHUB_API_VERSION), updateCaptor.capture()))
                    .thenReturn(buildApiResponse("closed", List.of(), List.of()));

            githubService.deleteIssue(jwt, ISSUE_ID);

            assertThat(updateCaptor.getValue().state()).isEqualTo("closed");
            verify(issueRepository).delete(existing);
        }

        @Test
        @DisplayName("deve deletar diretamente quando issue já está fechada")
        void deveDeletarDiretamente_quandoIssueJaEstaFechada() {
            Issue existing = buildPersistedIssue(State.CLOSED);
            when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(existing));

            githubService.deleteIssue(jwt, ISSUE_ID);

            verifyNoInteractions(githubIssueClient);
            verify(issueRepository).delete(existing);
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando issue não existe")
        void deveLancarDataNotFoundException_quandoIssueNaoExiste() {
            when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> githubService.deleteIssue(jwt, ISSUE_ID))
                    .isInstanceOf(DataNotFoundException.class);

            verifyNoInteractions(githubIssueClient);
            verify(issueRepository, never()).delete(any(Issue.class));
        }
    }

    @Nested
    @DisplayName("listUserRepositories")
    class ListUserRepositories {

        @Test
        @DisplayName("deve retornar tudo em uma única página quando há menos de 100 resultados")
        void deveRetornarTudoEmUmaUnicaPagina_quandoMenosDe100Resultados() {
            List<UserRepositoryResponse> page = repos(50);
            when(githubRepositoryClient.getUserRepositories(BEARER_GITHUB_TOKEN, GITHUB_API_VERSION, 100, 1))
                    .thenReturn(page);

            List<UserRepositoryResponse> result = githubService.listUserRepositories(GITHUB_TOKEN);

            assertThat(result).hasSize(50);
            verify(githubRepositoryClient, times(1)).getUserRepositories(anyString(), anyString(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("deve paginar até resposta parcial")
        void devePaginarAteRespostaParcial() {
            when(githubRepositoryClient.getUserRepositories(BEARER_GITHUB_TOKEN, GITHUB_API_VERSION, 100, 1)).thenReturn(repos(100));
            when(githubRepositoryClient.getUserRepositories(BEARER_GITHUB_TOKEN, GITHUB_API_VERSION, 100, 2)).thenReturn(repos(100));
            when(githubRepositoryClient.getUserRepositories(BEARER_GITHUB_TOKEN, GITHUB_API_VERSION, 100, 3)).thenReturn(repos(30));

            List<UserRepositoryResponse> result = githubService.listUserRepositories(GITHUB_TOKEN);

            assertThat(result).hasSize(230);
            verify(githubRepositoryClient, times(3)).getUserRepositories(anyString(), anyString(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("deve retornar lista vazia quando primeira página é vazia")
        void deveRetornarListaVazia_quandoPrimeiraPaginaEhVazia() {
            when(githubRepositoryClient.getUserRepositories(BEARER_GITHUB_TOKEN, GITHUB_API_VERSION, 100, 1))
                    .thenReturn(List.of());

            List<UserRepositoryResponse> result = githubService.listUserRepositories(GITHUB_TOKEN);

            assertThat(result).isEmpty();
            verify(githubRepositoryClient, times(1)).getUserRepositories(anyString(), anyString(), anyInt(), anyInt());
        }

        private List<UserRepositoryResponse> repos(int n) {
            return IntStream.range(0, n)
                    .mapToObj(i -> new UserRepositoryResponse((long) i, "repo-" + i, OWNER + "/repo-" + i, false, "d", "url"))
                    .toList();
        }
    }

    @Nested
    @DisplayName("checkIfRepositoryAndOwnerNameAreValid")
    class CheckIfRepositoryAndOwnerNameAreValid {

        @Test
        @DisplayName("deve retornar true quando client retorna repositório")
        void deveRetornarTrue_quandoClientRetornaRepositorio() {
            when(githubRepositoryClient.getRepository(OWNER, REPOSITORY, BEARER_GITHUB_TOKEN, GITHUB_API_VERSION))
                    .thenReturn(new RepositoryResponse("1", REPOSITORY, OWNER + "/" + REPOSITORY, "false"));

            boolean valid = githubService.checkIfRepositoryAndOwnerNameAreValid(GITHUB_TOKEN, OWNER, REPOSITORY);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("deve retornar false quando client retorna nulo")
        void deveRetornarFalse_quandoClientRetornaNulo() {
            when(githubRepositoryClient.getRepository(OWNER, REPOSITORY, BEARER_GITHUB_TOKEN, GITHUB_API_VERSION))
                    .thenReturn(null);

            boolean valid = githubService.checkIfRepositoryAndOwnerNameAreValid(GITHUB_TOKEN, OWNER, REPOSITORY);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("deve retornar false quando client lança exceção")
        void deveRetornarFalse_quandoClientLancaExcecao() {
            when(githubRepositoryClient.getRepository(OWNER, REPOSITORY, BEARER_GITHUB_TOKEN, GITHUB_API_VERSION))
                    .thenThrow(new RuntimeException("404"));

            boolean valid = githubService.checkIfRepositoryAndOwnerNameAreValid(GITHUB_TOKEN, OWNER, REPOSITORY);

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("createRepositoryWebhook")
    class CreateRepositoryWebhook {

        @Test
        @DisplayName("deve criar webhook e retornar id quando base URL é pública")
        void deveCriarWebhookERetornarId_quandoBaseUrlEhPublica() {
            ArgumentCaptor<GithubWebhookCreateRequest> captor = ArgumentCaptor.forClass(GithubWebhookCreateRequest.class);
            when(githubWebhookClient.createWebhook(eq(OWNER), eq(REPOSITORY), eq(BEARER_GITHUB_TOKEN), eq(GITHUB_API_VERSION), captor.capture()))
                    .thenReturn(new GithubWebhookResponse(555L, "url", "web", List.of("issues"), true));

            Long webhookId = githubService.createRepositoryWebhook(GITHUB_TOKEN, OWNER, REPOSITORY);

            assertThat(webhookId).isEqualTo(555L);
            GithubWebhookCreateRequest sent = captor.getValue();
            assertThat(sent.name()).isEqualTo("web");
            assertThat(sent.events()).containsExactly("issues");
            assertThat(sent.active()).isTrue();
            assertThat(sent.config().url()).isEqualTo("https://planora.example.com/v1/webhook/github/issues");
            assertThat(sent.config().contentType()).isEqualTo("json");
            assertThat(sent.config().insecureSsl()).isEqualTo("0");
            assertThat(sent.config().secret()).isNull();
        }

        @Test
        @DisplayName("deve passar secret quando webhookSecret está configurado")
        void devePassarSecret_quandoWebhookSecretConfigurado() {
            ReflectionTestUtils.setField(githubService, "webhookSecret", "s3cret");
            ArgumentCaptor<GithubWebhookCreateRequest> captor = ArgumentCaptor.forClass(GithubWebhookCreateRequest.class);
            when(githubWebhookClient.createWebhook(eq(OWNER), eq(REPOSITORY), eq(BEARER_GITHUB_TOKEN), eq(GITHUB_API_VERSION), captor.capture()))
                    .thenReturn(new GithubWebhookResponse(1L, "url", "web", List.of("issues"), true));

            githubService.createRepositoryWebhook(GITHUB_TOKEN, OWNER, REPOSITORY);

            assertThat(captor.getValue().config().secret()).isEqualTo("s3cret");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "http://localhost:8080",
                "http://127.0.0.1:8080",
                "http://0.0.0.0:8080",
                "https://LOCALHOST.dev"
        })
        @DisplayName("deve retornar null sem chamar client quando base URL é local")
        void deveRetornarNullSemChamarClient_quandoBaseUrlEhLocal(String localUrl) {
            ReflectionTestUtils.setField(githubService, "appBaseUrl", localUrl);

            Long webhookId = githubService.createRepositoryWebhook(GITHUB_TOKEN, OWNER, REPOSITORY);

            assertThat(webhookId).isNull();
            verifyNoInteractions(githubWebhookClient);
        }

        @Test
        @DisplayName("deve construir URL preservando porta customizada")
        void deveConstruirUrlComPortaCustomizada() {
            ReflectionTestUtils.setField(githubService, "appBaseUrl", "https://app.example.com:9000");
            ArgumentCaptor<GithubWebhookCreateRequest> captor = ArgumentCaptor.forClass(GithubWebhookCreateRequest.class);
            when(githubWebhookClient.createWebhook(eq(OWNER), eq(REPOSITORY), eq(BEARER_GITHUB_TOKEN), eq(GITHUB_API_VERSION), captor.capture()))
                    .thenReturn(new GithubWebhookResponse(1L, "url", "web", List.of("issues"), true));

            githubService.createRepositoryWebhook(GITHUB_TOKEN, OWNER, REPOSITORY);

            assertThat(captor.getValue().config().url())
                    .isEqualTo("https://app.example.com:9000/v1/webhook/github/issues");
        }
    }

    @Nested
    @DisplayName("deleteRepositoryWebhook")
    class DeleteRepositoryWebhook {

        @Test
        @DisplayName("deve delegar para o cliente quando chamado")
        void deveDelegarParaCliente_quandoChamado() {
            githubService.deleteRepositoryWebhook(GITHUB_TOKEN, OWNER, REPOSITORY, 99L);

            verify(githubWebhookClient).deleteWebhook(OWNER, REPOSITORY, 99L, BEARER_GITHUB_TOKEN, GITHUB_API_VERSION);
        }

        @Test
        @DisplayName("não deve propagar exceção quando client falha")
        void naoDevePropagarExcecao_quandoClientFalha() {
            doThrowOnDelete();

            assertThatCode(() -> githubService.deleteRepositoryWebhook(GITHUB_TOKEN, OWNER, REPOSITORY, 99L))
                    .doesNotThrowAnyException();
        }

        private void doThrowOnDelete() {
            org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                    .when(githubWebhookClient)
                    .deleteWebhook(OWNER, REPOSITORY, 99L, BEARER_GITHUB_TOKEN, GITHUB_API_VERSION);
        }
    }
}
