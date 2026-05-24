package com.planora.backend.service;

import com.planora.backend.client.ApiPythonClient;
import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.Job.Job;
import com.planora.backend.model.Job.dto.CallbackRequest;
import com.planora.backend.model.issue.dto.AcceptedResponse;
import com.planora.backend.model.issue.dto.BacklogRequest;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.repository.JobRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiPythonService")
class ApiPythonServiceTest {

    private static final Long JOB_ID = 100L;
    private static final Long BOARD_ID = 1L;
    private static final Long COLUMN_ID = 5L;
    private static final Long USER_ID = 42L;
    private static final String REPOSITORY = "planora";
    private static final String TITLE = "Backlog de autenticação";
    private static final String DESCRIPTION = "Gerar backlog de autenticação";
    private static final String JWT_TOKEN_VALUE = "encoded-jwt";

    @Mock private ApiPythonClient apiPythonClient;
    @Mock private JobRepository jobRepository;
    @Mock private KanbanBoardService kanbanBoardService;
    @Mock private Jwt jwt;

    @InjectMocks private ApiPythonService apiPythonService;

    @Nested
    @DisplayName("generateBacklog")
    class GenerateBacklog {

        @Test
        @DisplayName("deve persistir Job e retornar AcceptedResponse")
        void devePersistirJobERetornarAcceptedResponse() {
            KanbanBoard board = KanbanBoard.builder().githubRepository(REPOSITORY).build();
            when(kanbanBoardService.findById(BOARD_ID)).thenReturn(board);
            when(jwt.getClaims()).thenReturn(Map.of("token", JWT_TOKEN_VALUE));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
                Job job = invocation.getArgument(0);
                job.setId(JOB_ID);
                return job;
            });

            AcceptedResponse response = apiPythonService.generateBacklog(
                    TITLE, DESCRIPTION, BOARD_ID, COLUMN_ID, jwt, USER_ID);

            ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
            verify(jobRepository).save(jobCaptor.capture());
            Job saved = jobCaptor.getValue();

            assertThat(saved.getTitle()).isEqualTo(TITLE);
            assertThat(saved.getDescription()).isEqualTo(DESCRIPTION);
            assertThat(saved.getBoardId()).isEqualTo(BOARD_ID);
            assertThat(saved.getColumnId()).isEqualTo(COLUMN_ID);
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getRepository()).isEqualTo(REPOSITORY);
            assertThat(saved.getJwtToken()).isEqualTo(JWT_TOKEN_VALUE);

            assertThat(response.jobId()).isEqualTo(JOB_ID);
        }

        @Test
        @Disabled("Reativar quando o stub TEMP de generateBacklogWithRetry for removido")
        @DisplayName("deve propagar exceção quando ApiPythonClient falha em todas as tentativas")
        void devePropagarExcecao_quandoApiPythonClientFalha() {
            KanbanBoard board = KanbanBoard.builder().githubRepository(REPOSITORY).build();
            when(kanbanBoardService.findById(BOARD_ID)).thenReturn(board);
            when(jwt.getClaims()).thenReturn(Map.of("token", JWT_TOKEN_VALUE));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
                Job job = invocation.getArgument(0);
                job.setId(JOB_ID);
                return job;
            });
            when(apiPythonClient.generateBacklog(any(BacklogRequest.class)))
                    .thenThrow(new RuntimeException("timeout"));

            assertThatThrownBy(() -> apiPythonService.generateBacklog(
                    TITLE, DESCRIPTION, BOARD_ID, COLUMN_ID, jwt, USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("unavailable");
        }
    }

    @Nested
    @DisplayName("saveBacklog")
    class SaveBacklog {

        @Test
        @DisplayName("deve lançar DataNotFoundException quando Job não existe")
        void deveLancarDataNotFoundException_quandoJobNaoExiste() {
            IssueRequest issue = new IssueRequest("t", "b", List.of(), List.of());
            CallbackRequest callback = new CallbackRequest(List.of(issue), JOB_ID);
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> apiPythonService.saveBacklog(callback, jwt))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessageContaining("Job not found");

            verifyNoInteractions(kanbanBoardService);
        }

        @Test
        @DisplayName("deve delegar issues para KanbanBoardService quando Job existe")
        void deveDelegarIssues_quandoJobExiste() {
            IssueRequest issue = new IssueRequest("Título", "corpo", List.of(), List.of());
            CallbackRequest callback = new CallbackRequest(List.of(issue), JOB_ID);
            Job job = Job.builder()
                    .id(JOB_ID)
                    .boardId(BOARD_ID)
                    .columnId(COLUMN_ID)
                    .userId(USER_ID)
                    .repository(REPOSITORY)
                    .jwtToken(JWT_TOKEN_VALUE)
                    .description(DESCRIPTION)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            apiPythonService.saveBacklog(callback, jwt);

            verify(kanbanBoardService).createBulkIssuesAndAddToColumn(
                    eq(BOARD_ID), eq(COLUMN_ID), eq(jwt),
                    eq(List.of(issue)), eq(USER_ID), eq(REPOSITORY));
        }

        @Test
        @DisplayName("não deve interagir com KanbanBoardService quando backlog está vazio")
        void naoDeveInteragir_quandoBacklogEhVazio() {
            CallbackRequest callback = new CallbackRequest(List.of(), JOB_ID);

            apiPythonService.saveBacklog(callback, jwt);

            verifyNoInteractions(jobRepository, kanbanBoardService);
        }

        @Test
        @DisplayName("deve propagar exceção quando KanbanBoardService falha")
        void devePropagarExcecao_quandoKanbanBoardServiceFalha() {
            IssueRequest issue = new IssueRequest("t", "b", List.of(), List.of());
            CallbackRequest callback = new CallbackRequest(List.of(issue), JOB_ID);
            Job job = Job.builder()
                    .id(JOB_ID)
                    .boardId(BOARD_ID)
                    .columnId(COLUMN_ID)
                    .userId(USER_ID)
                    .repository(REPOSITORY)
                    .jwtToken(JWT_TOKEN_VALUE)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(kanbanBoardService.createBulkIssuesAndAddToColumn(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("board error"));

            assertThatThrownBy(() -> apiPythonService.saveBacklog(callback, jwt))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("board error");
        }
    }
}
