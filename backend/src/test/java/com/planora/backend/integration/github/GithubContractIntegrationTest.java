package com.planora.backend.integration.github;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.planora.backend.integration.AbstractIntegrationTest;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.dto.IssueRequest;
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

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GitHub Contract Integration — Backend <-> GitHub API (WireMock)")
class GithubContractIntegrationTest extends AbstractIntegrationTest {

    private static final String OWNER = "llucascr";
    private static final String REPO = "planora";
    private static final String GITHUB_TOKEN = "ghp_contract_token_abc123";

    private static WireMockServer wireMockServer;

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
        registry.add("github.api.base-url", () -> "http://localhost:" + wireMockServer.port());
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    @DisplayName("POST /board/issue/create sends correct payload (title, Markdown body, labels) and Bearer token to GitHub")
    void createIssue_sendsCorrectContractToGithub() throws Exception {
        User owner = persistUser(OWNER, GITHUB_TOKEN);
        KanbanBoard board = persistBoardWithColumns(owner, OWNER, REPO);
        KanbanColumn todo = kanbanColumnRepository
                .findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(board.getKanbanBoardId())
                .stream().filter(c -> c.getPosition() == 0).findFirst().orElseThrow();

        String markdownBody = "## Steps to reproduce\n\n1. Login\n2. Click button\n\n**Expected:** success\n**Actual:** error";
        String issueApiResponse = """
                {
                  "url": "https://api.github.com/repos/%s/%s/issues/42",
                  "number": 42,
                  "title": "Bug: login broken",
                  "body": %s,
                  "state": "open",
                  "user": {"login": "%s", "avatar_url": "", "email": "", "notification_email": ""},
                  "labels": [{"url": "", "name": "bug", "color": "ff0000", "description": "Bug label"}],
                  "assignees": []
                }
                """.formatted(OWNER, REPO, objectMapper.writeValueAsString(markdownBody), OWNER);

        wireMockServer.stubFor(WireMock.post(urlMatching("/repos/" + OWNER + "/" + REPO + "/issues"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(issueApiResponse)));

        IssueRequest request = new IssueRequest(
                "Bug: login broken",
                markdownBody,
                List.of(),
                List.of("bug")
        );

        mockMvc.perform(post("/v1/kanban/board/issue/create")
                        .with(jwtFor(owner))
                        .param("boardId", board.getKanbanBoardId().toString())
                        .param("columnId", todo.getKanbanColumnId().toString())
                        .param("repository", REPO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated());

        List<LoggedRequest> captured = wireMockServer.findAll(
                RequestPatternBuilder.newRequestPattern(
                        com.github.tomakehurst.wiremock.http.RequestMethod.POST,
                        urlMatching("/repos/" + OWNER + "/" + REPO + "/issues")));
        assertThat(captured).hasSize(1);
        LoggedRequest gh = captured.get(0);

        // Headers: Bearer token + GitHub API version
        assertThat(gh.getHeader("Authorization")).isEqualTo("Bearer " + GITHUB_TOKEN);
        assertThat(gh.getHeader("X-GitHub-Api-Version")).isEqualTo("2022-11-28");

        // Body: title, body in Markdown preserved char-for-char, labels in order
        String sentBody = gh.getBodyAsString();
        assertThat(sentBody).contains("\"title\":\"Bug: login broken\"");
        assertThat(sentBody).contains("\"labels\":[\"bug\"]");
        // Markdown body preserved literally (Jackson escapes newlines as \n)
        assertThat(sentBody).contains("## Steps to reproduce");
        assertThat(sentBody).contains("**Expected:** success");
        assertThat(sentBody).contains("**Actual:** error");

        // Persisted issue reflects the GitHub response
        List<Issue> issues = issueRepository.findAll();
        assertThat(issues).hasSize(1);
        Issue persisted = issues.get(0);
        assertThat(persisted.getNumber()).isEqualTo(42);
        assertThat(persisted.getTitle()).isEqualTo("Bug: login broken");
        assertThat(persisted.getBody()).isEqualTo(markdownBody);
        assertThat(persisted.getLabels()).hasSize(1);
        assertThat(persisted.getLabels().get(0).getName()).isEqualTo("bug");
    }

    @Test
    @DisplayName("PATCH /board/issue/{id}/close sends state=closed to GitHub via PATCH /repos/.../issues/{number}")
    void closeIssue_sendsStateClosedToGithub() throws Exception {
        User owner = persistUser(OWNER, GITHUB_TOKEN);
        KanbanBoard board = persistBoardWithColumns(owner, OWNER, REPO);
        KanbanColumn todo = kanbanColumnRepository
                .findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(board.getKanbanBoardId())
                .stream().filter(c -> c.getPosition() == 0).findFirst().orElseThrow();
        Issue issue = persistIssue(todo, owner, "To close", "Body", 7);

        String patchResponse = """
                {
                  "url": "https://api.github.com/repos/%s/%s/issues/7",
                  "number": 7,
                  "title": "To close",
                  "body": "Body",
                  "state": "closed",
                  "user": {"login": "%s", "avatar_url": "", "email": "", "notification_email": ""},
                  "labels": [],
                  "assignees": []
                }
                """.formatted(OWNER, REPO, OWNER);

        wireMockServer.stubFor(WireMock.patch(urlMatching("/repos/" + OWNER + "/" + REPO + "/issues/7"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(patchResponse)));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/v1/kanban/board/issue/" + issue.getIssueId() + "/close")
                        .with(jwtFor(owner)))
                .andExpect(status().isOk());

        List<LoggedRequest> captured = wireMockServer.findAll(
                RequestPatternBuilder.newRequestPattern(
                        com.github.tomakehurst.wiremock.http.RequestMethod.PATCH,
                        urlMatching("/repos/" + OWNER + "/" + REPO + "/issues/7")));
        assertThat(captured).hasSize(1);
        String body = captured.get(0).getBodyAsString();
        assertThat(body).contains("\"state\":\"closed\"");
        assertThat(captured.get(0).getHeader("Authorization")).isEqualTo("Bearer " + GITHUB_TOKEN);

        Issue refreshed = issueRepository.findById(issue.getIssueId()).orElseThrow();
        assertThat(refreshed.getState()).isEqualTo(com.planora.backend.model.issue.State.CLOSED);
        assertThat(refreshed.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("createIssue propagates GitHub 422 error to the client with mapped status")
    void createIssue_propagatesGithubErrorStatus() throws Exception {
        User owner = persistUser(OWNER, GITHUB_TOKEN);
        KanbanBoard board = persistBoardWithColumns(owner, OWNER, REPO);
        KanbanColumn todo = kanbanColumnRepository
                .findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(board.getKanbanBoardId())
                .stream().filter(c -> c.getPosition() == 0).findFirst().orElseThrow();

        wireMockServer.stubFor(WireMock.post(urlMatching("/repos/.+/.+/issues"))
                .willReturn(aResponse()
                        .withStatus(422)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"Validation Failed\"}")));

        IssueRequest request = new IssueRequest("title", "body", List.of(), List.of());

        // WebClientResponseException is mapped to the same upstream status by CustomEntityResponseHandler.
        mockMvc.perform(post("/v1/kanban/board/issue/create")
                        .with(jwtFor(owner))
                        .param("boardId", board.getKanbanBoardId().toString())
                        .param("columnId", todo.getKanbanColumnId().toString())
                        .param("repository", REPO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().is4xxClientError());

        assertThat(issueRepository.count()).isZero();
    }
}
