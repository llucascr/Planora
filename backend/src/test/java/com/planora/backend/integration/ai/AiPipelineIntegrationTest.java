package com.planora.backend.integration.ai;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.planora.backend.client.GithubIssueClient;
import com.planora.backend.client.GithubLabelClient;
import com.planora.backend.client.GithubRepositoryClient;
import com.planora.backend.client.GithubSearchClient;
import com.planora.backend.client.GithubWebhookClient;
import com.planora.backend.integration.AbstractIntegrationTest;
import com.planora.backend.model.Job.Job;
import com.planora.backend.model.Job.JobStatus;
import com.planora.backend.model.Job.dto.CallbackRequest;
import com.planora.backend.model.Job.dto.GenerateBacklogRequest;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.dto.IssueApiResponse;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.RepositoryResponse;
import com.planora.backend.model.issue.dto.UserIssueResponse;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.user.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AI Pipeline Integration — Backend <-> Python AI server")
class AiPipelineIntegrationTest extends AbstractIntegrationTest {

    private static final String OWNER = "llucascr";
    private static final String REPO = "planora";

    private static WireMockServer wireMockServer;

    @MockitoBean private GithubIssueClient githubIssueClient;
    @MockitoBean private GithubRepositoryClient githubRepositoryClient;
    @MockitoBean private GithubLabelClient githubLabelClient;
    @MockitoBean private GithubWebhookClient githubWebhookClient;
    @MockitoBean private GithubSearchClient githubSearchClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void overrideBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("api-python.base-url", () -> "http://localhost:" + wireMockServer.port() + "/api/v1");
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    @DisplayName("POST /v1/ia persists Job and forwards Alpaca-shaped payload to Python server")
    void generateBacklog_persistsJobAndForwardsAlpacaPayload() throws Exception {
        User owner = persistUser(OWNER, "ghp-token");
        KanbanBoard board = persistBoardWithColumns(owner, OWNER, REPO);
        KanbanColumn todo = kanbanColumnRepository
                .findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(board.getKanbanBoardId())
                .stream().filter(c -> c.getPosition() == 0).findFirst().orElseThrow();

        wireMockServer.stubFor(WireMock.post(urlEqualTo("/api/v1/generate-backlog"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"jobId\":0,\"message\":\"accepted\"}")));

        GenerateBacklogRequest request = new GenerateBacklogRequest("Build kanban Planora");

        mockMvc.perform(post("/v1/ia")
                        .with(jwtFor(owner))
                        .param("title", "Planora MVP")
                        .param("boardId", board.getKanbanBoardId().toString())
                        .param("columnId", todo.getKanbanColumnId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("accepted"));

        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);
        Job persisted = jobs.get(0);
        assertThat(persisted.getTitle()).isEqualTo("Planora MVP");
        assertThat(persisted.getBoardId()).isEqualTo(board.getKanbanBoardId());
        assertThat(persisted.getColumnId()).isEqualTo(todo.getKanbanColumnId());
        assertThat(persisted.getUserId()).isEqualTo(owner.getUserId());
        assertThat(persisted.getRepository()).isEqualTo(REPO);
        // Description stored as JSON-stringified Alpaca preprocessing input.
        assertThat(persisted.getDescription()).isEqualTo("{\"description\":\"Build kanban Planora\"}");

        List<LoggedRequest> captured = wireMockServer.findAll(
                RequestPatternBuilder.newRequestPattern(
                        com.github.tomakehurst.wiremock.http.RequestMethod.POST,
                        urlEqualTo("/api/v1/generate-backlog")));
        assertThat(captured).hasSize(1);
        String body = captured.get(0).getBodyAsString();
        assertThat(body).contains("\"jobId\":" + persisted.getId());
        // The "description" field carries the Alpaca-shaped JSON. Jackson escapes the inner quotes.
        assertThat(body).contains("\"description\":\"{\\\"description\\\":\\\"Build kanban Planora\\\"}\"");
    }

    @Test
    @DisplayName("POST /v1/ia/callback parses model JSON and creates issues for each backlog item")
    void callback_parsesBacklogAndCreatesIssues() throws Exception {
        User owner = persistUser(OWNER, "ghp-token");
        KanbanBoard board = persistBoardWithColumns(owner, OWNER, REPO);
        KanbanColumn todo = kanbanColumnRepository
                .findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(board.getKanbanBoardId())
                .stream().filter(c -> c.getPosition() == 0).findFirst().orElseThrow();

        Job job = Job.builder()
                .title("Planora MVP")
                .description("{\"description\":\"build it\"}")
                .boardId(board.getKanbanBoardId())
                .columnId(todo.getKanbanColumnId())
                .userId(owner.getUserId())
                .repository(REPO)
                .status(JobStatus.PROCESSING)
                .jwtToken("fake-token")
                .build();
        job = jobRepository.save(job);

        when(githubRepositoryClient.getRepository(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new RepositoryResponse("1", REPO, OWNER + "/" + REPO, "false"));
        when(githubIssueClient.createIssue(anyString(), anyString(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> {
                    IssueRequest req = invocation.getArgument(4);
                    return new IssueApiResponse(
                            "https://github.com/x/y/issues/1", 1,
                            req.title(), req.body(), "open",
                            new UserIssueResponse(OWNER, "", "", ""),
                            List.of(), List.of()
                    );
                });

        CallbackRequest callback = new CallbackRequest(
                List.of(
                        new IssueRequest("Setup OAuth", "## Steps\n\n1. Configure provider", List.of(), List.of()),
                        new IssueRequest("Add Kanban UI", "Drag and drop interface", List.of(), List.of())
                ),
                job.getId()
        );

        mockMvc.perform(post("/v1/ia/callback")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(callback)))
                .andExpect(status().isOk());

        List<Issue> issues = issueRepository.findAll();
        assertThat(issues).hasSize(2);
        assertThat(issues).extracting(Issue::getTitle)
                .containsExactlyInAnyOrder("Setup OAuth", "Add Kanban UI");
        assertThat(issues).allSatisfy(i ->
                assertThat(i.getColumn().getKanbanColumnId()).isEqualTo(todo.getKanbanColumnId()));

        Job refreshed = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    @DisplayName("POST /v1/ia/callback with empty backlog returns 200 without creating issues")
    void callback_emptyBacklogIsIgnored() throws Exception {
        User owner = persistUser(OWNER, "ghp-token");
        KanbanBoard board = persistBoardWithColumns(owner, OWNER, REPO);
        KanbanColumn todo = kanbanColumnRepository
                .findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(board.getKanbanBoardId())
                .stream().filter(c -> c.getPosition() == 0).findFirst().orElseThrow();

        Job job = jobRepository.save(Job.builder()
                .title("Empty")
                .description("{}")
                .boardId(board.getKanbanBoardId())
                .columnId(todo.getKanbanColumnId())
                .userId(owner.getUserId())
                .repository(REPO)
                .status(JobStatus.PROCESSING)
                .jwtToken("fake-token")
                .build());

        CallbackRequest callback = new CallbackRequest(List.of(), job.getId());

        mockMvc.perform(post("/v1/ia/callback")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(callback)))
                .andExpect(status().isOk());

        assertThat(issueRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /v1/ia/callback with unknown jobId returns 500 (DataNotFoundException wrapped)")
    void callback_unknownJobReturnsServerError() throws Exception {
        User owner = persistUser(OWNER, "ghp-token");
        CallbackRequest callback = new CallbackRequest(
                List.of(new IssueRequest("X", "Y", List.of(), List.of())),
                9999L
        );

        mockMvc.perform(post("/v1/ia/callback")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(callback)))
                .andExpect(status().is5xxServerError());

        assertThat(issueRepository.count()).isZero();
    }
}
