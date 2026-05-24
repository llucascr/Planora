package com.planora.backend.service;

import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.Label;
import com.planora.backend.model.issue.State;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.IssueRepository;
import com.planora.backend.repository.KanbanBoardRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IssueEventHandler")
class IssueEventHandlerTest {

    private static final String OWNER = "llucascr";
    private static final String REPO = "planora";
    private static final Integer ISSUE_NUMBER = 123;
    private static final Long BOARD_ID = 1L;

    @Mock private KanbanBoardRepository kanbanBoardRepository;
    @Mock private IssueRepository issueRepository;
    @Mock private LabelService labelService;
    @Mock private UserService userService;

    @InjectMocks private IssueEventHandler issueEventHandler;

    private KanbanBoard board;
    private KanbanColumn firstColumn;
    private KanbanColumn secondColumn;

    @BeforeEach
    void setUp() {
        firstColumn = KanbanColumn.builder()
                .kanbanColumnId(10L)
                .name("Todo")
                .position(0)
                .build();
        secondColumn = KanbanColumn.builder()
                .kanbanColumnId(11L)
                .name("Done")
                .position(1)
                .build();
        board = KanbanBoard.builder()
                .kanbanBoardId(BOARD_ID)
                .githubOwnerName(OWNER)
                .githubRepository(REPO)
                .columns(List.of(secondColumn, firstColumn))
                .build();
    }

    private String buildIssuePayload(String action, Integer number, String state,
                                     String labelsJson, String assigneesJson, String closedAt) {
        return """
                {
                  "action": "%s",
                  "issue": {
                    "number": %d,
                    "title": "Título da issue",
                    "body": "Corpo da issue",
                    "state": "%s",
                    "url": "https://api.github.com/repos/%s/%s/issues/%d",
                    "labels": %s,
                    "assignees": %s,
                    "created_at": "2026-05-01T12:00:00Z",
                    "updated_at": "2026-05-02T12:00:00Z",
                    "closed_at": %s
                  },
                  "repository": {
                    "name": "%s",
                    "owner": { "login": "%s" }
                  }
                }
                """.formatted(action, number, state, OWNER, REPO, number,
                labelsJson, assigneesJson,
                closedAt == null ? "null" : "\"" + closedAt + "\"",
                REPO, OWNER);
    }

    private String simplePayload(String state, String closedAt) {
        return buildIssuePayload("opened", ISSUE_NUMBER, state, "[]", "[]", closedAt);
    }

    @Nested
    @DisplayName("handle — early exits")
    class EarlyExits {

        @Test
        @DisplayName("deve ignorar quando payload JSON é inválido")
        void deveIgnorar_quandoPayloadJsonInvalido() {
            issueEventHandler.handle("{ this is not json");

            verifyNoInteractions(kanbanBoardRepository, issueRepository);
        }

        @Test
        @DisplayName("deve ignorar quando payload não tem issue ou repository")
        void deveIgnorar_quandoSemIssueOuRepository() {
            issueEventHandler.handle("{\"action\":\"opened\"}");

            verifyNoInteractions(kanbanBoardRepository, issueRepository);
        }

        @Test
        @DisplayName("deve ignorar quando nenhum board está vinculado ao repositório")
        void deveIgnorar_quandoNenhumBoardParaRepo() {
            String payload = simplePayload("open", null);
            when(kanbanBoardRepository.findByGithubOwnerAndRepositoryIgnoreCase(OWNER, REPO))
                    .thenReturn(List.of());

            issueEventHandler.handle(payload);

            verifyNoInteractions(issueRepository);
        }
    }

    @Nested
    @DisplayName("handle — sincronização de issue")
    class IssueSync {

        @Test
        @DisplayName("deve criar issue na column de menor position quando issue não existe no board")
        void deveCriarIssueNaPrimeiraColumn_quandoIssueNaoExiste() {
            String payload = simplePayload("open", null);
            when(kanbanBoardRepository.findByGithubOwnerAndRepositoryIgnoreCase(OWNER, REPO))
                    .thenReturn(List.of(board));
            when(issueRepository.findByNumberAndBoardId(ISSUE_NUMBER, BOARD_ID))
                    .thenReturn(Optional.empty());

            issueEventHandler.handle(payload);

            ArgumentCaptor<Issue> captor = ArgumentCaptor.forClass(Issue.class);
            verify(issueRepository).save(captor.capture());
            Issue saved = captor.getValue();

            assertThat(saved.getNumber()).isEqualTo(ISSUE_NUMBER);
            assertThat(saved.getTitle()).isEqualTo("Título da issue");
            assertThat(saved.getBody()).isEqualTo("Corpo da issue");
            assertThat(saved.getState()).isEqualTo(State.OPEN);
            assertThat(saved.getColumn()).isSameAs(firstColumn);
            assertThat(saved.getLabels()).isEmpty();
            assertThat(saved.getAssignees()).isEmpty();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("deve resolver labels do payload via LabelService ao criar issue")
        void deveResolverLabels_aoCriarIssue() {
            String labelsJson = "[{\"url\":\"u\",\"name\":\"bug\",\"color\":\"f00\",\"description\":\"d\"}]";
            String payload = buildIssuePayload("opened", ISSUE_NUMBER, "open", labelsJson, "[]", null);
            Label persistedLabel = new Label();
            persistedLabel.setName("bug");
            when(kanbanBoardRepository.findByGithubOwnerAndRepositoryIgnoreCase(OWNER, REPO))
                    .thenReturn(List.of(board));
            when(issueRepository.findByNumberAndBoardId(ISSUE_NUMBER, BOARD_ID))
                    .thenReturn(Optional.empty());
            when(labelService.resolveOrCreateLabel(any())).thenReturn(persistedLabel);

            issueEventHandler.handle(payload);

            ArgumentCaptor<Issue> captor = ArgumentCaptor.forClass(Issue.class);
            verify(issueRepository).save(captor.capture());
            assertThat(captor.getValue().getLabels()).containsExactly(persistedLabel);
        }

        @Test
        @DisplayName("não deve criar issue quando board não tem colunas")
        void naoDeveCriarIssue_quandoBoardSemColunas() {
            board.setColumns(List.of());
            String payload = simplePayload("open", null);
            when(kanbanBoardRepository.findByGithubOwnerAndRepositoryIgnoreCase(OWNER, REPO))
                    .thenReturn(List.of(board));
            when(issueRepository.findByNumberAndBoardId(ISSUE_NUMBER, BOARD_ID))
                    .thenReturn(Optional.empty());

            issueEventHandler.handle(payload);

            verify(issueRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve atualizar issue existente quando já está no banco")
        void deveAtualizarIssueExistente_quandoJaEstaNoBanco() {
            String payload = buildIssuePayload("edited", ISSUE_NUMBER, "open", "[]", "[]", null);
            Issue existing = new Issue();
            existing.setIssueId(99L);
            existing.setNumber(ISSUE_NUMBER);
            existing.setTitle("Antigo");
            existing.setState(State.CLOSED);
            existing.setColumn(firstColumn);
            when(kanbanBoardRepository.findByGithubOwnerAndRepositoryIgnoreCase(OWNER, REPO))
                    .thenReturn(List.of(board));
            when(issueRepository.findByNumberAndBoardId(ISSUE_NUMBER, BOARD_ID))
                    .thenReturn(Optional.of(existing));

            issueEventHandler.handle(payload);

            assertThat(existing.getTitle()).isEqualTo("Título da issue");
            assertThat(existing.getBody()).isEqualTo("Corpo da issue");
            assertThat(existing.getState()).isEqualTo(State.OPEN);
            assertThat(existing.getUpdatedAt()).isNotNull();
            verify(issueRepository).save(existing);
        }

        @Test
        @DisplayName("deve setar closedAt ao atualizar issue quando closed_at vem preenchido")
        void deveSetarClosedAt_quandoClosedAtPresente() {
            String payload = buildIssuePayload("closed", ISSUE_NUMBER, "closed", "[]", "[]", "2026-05-10T08:30:00Z");
            Issue existing = new Issue();
            existing.setIssueId(99L);
            existing.setNumber(ISSUE_NUMBER);
            existing.setState(State.OPEN);
            when(kanbanBoardRepository.findByGithubOwnerAndRepositoryIgnoreCase(OWNER, REPO))
                    .thenReturn(List.of(board));
            when(issueRepository.findByNumberAndBoardId(ISSUE_NUMBER, BOARD_ID))
                    .thenReturn(Optional.of(existing));

            issueEventHandler.handle(payload);

            assertThat(existing.getState()).isEqualTo(State.CLOSED);
            assertThat(existing.getClosedAt()).isEqualTo(LocalDateTime.of(2026, 5, 10, 8, 30));
        }

        @Test
        @DisplayName("deve NÃO setar closedAt ao atualizar issue quando closed_at é null")
        void naoDeveSetarClosedAt_quandoClosedAtNull() {
            String payload = simplePayload("open", null);
            Issue existing = new Issue();
            existing.setIssueId(99L);
            existing.setNumber(ISSUE_NUMBER);
            existing.setState(State.OPEN);
            existing.setClosedAt(null);
            when(kanbanBoardRepository.findByGithubOwnerAndRepositoryIgnoreCase(OWNER, REPO))
                    .thenReturn(List.of(board));
            when(issueRepository.findByNumberAndBoardId(ISSUE_NUMBER, BOARD_ID))
                    .thenReturn(Optional.of(existing));

            issueEventHandler.handle(payload);

            assertThat(existing.getClosedAt()).isNull();
        }

        @Test
        @DisplayName("deve resolver assignees filtrando apenas os existentes no banco")
        void deveResolverAssigneesFiltrandoExistentes() {
            String assigneesJson = "[{\"login\":\"alice\"},{\"login\":\"ghost\"}]";
            String payload = buildIssuePayload("edited", ISSUE_NUMBER, "open", "[]", assigneesJson, null);
            User alice = User.builder().userId(2L).login("alice").build();
            Issue existing = new Issue();
            existing.setIssueId(99L);
            existing.setNumber(ISSUE_NUMBER);
            existing.setState(State.OPEN);
            when(kanbanBoardRepository.findByGithubOwnerAndRepositoryIgnoreCase(OWNER, REPO))
                    .thenReturn(List.of(board));
            when(issueRepository.findByNumberAndBoardId(ISSUE_NUMBER, BOARD_ID))
                    .thenReturn(Optional.of(existing));
            when(userService.findOptionalByLogin("alice")).thenReturn(Optional.of(alice));
            when(userService.findOptionalByLogin("ghost")).thenReturn(Optional.empty());

            issueEventHandler.handle(payload);

            assertThat(existing.getAssignees()).containsExactly(alice);
        }

        @ParameterizedTest
        @ValueSource(strings = {"open", "closed"})
        @DisplayName("deve mapear state da API para enum State em maiúsculo")
        void deveMapearStateParaEnum(String stateInput) {
            String payload = buildIssuePayload("any", ISSUE_NUMBER, stateInput, "[]", "[]", null);
            when(kanbanBoardRepository.findByGithubOwnerAndRepositoryIgnoreCase(OWNER, REPO))
                    .thenReturn(List.of(board));
            when(issueRepository.findByNumberAndBoardId(ISSUE_NUMBER, BOARD_ID))
                    .thenReturn(Optional.empty());

            issueEventHandler.handle(payload);

            ArgumentCaptor<Issue> captor = ArgumentCaptor.forClass(Issue.class);
            verify(issueRepository).save(captor.capture());
            assertThat(captor.getValue().getState()).isEqualTo(State.valueOf(stateInput.toUpperCase()));
        }

        @Test
        @DisplayName("deve sincronizar issue em todos os boards retornados pelo repositório")
        void deveSincronizarIssueEmTodosOsBoards() {
            KanbanColumn col2 = KanbanColumn.builder().kanbanColumnId(20L).position(0).build();
            KanbanBoard board2 = KanbanBoard.builder()
                    .kanbanBoardId(2L)
                    .githubOwnerName(OWNER)
                    .githubRepository(REPO)
                    .columns(List.of(col2))
                    .build();
            String payload = simplePayload("open", null);
            when(kanbanBoardRepository.findByGithubOwnerAndRepositoryIgnoreCase(OWNER, REPO))
                    .thenReturn(List.of(board, board2));
            lenient().when(issueRepository.findByNumberAndBoardId(ISSUE_NUMBER, BOARD_ID))
                    .thenReturn(Optional.empty());
            lenient().when(issueRepository.findByNumberAndBoardId(ISSUE_NUMBER, 2L))
                    .thenReturn(Optional.empty());

            issueEventHandler.handle(payload);

            verify(issueRepository, times(2)).save(any(Issue.class));
        }
    }
}
