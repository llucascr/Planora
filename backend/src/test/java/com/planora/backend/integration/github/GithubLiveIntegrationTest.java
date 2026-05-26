package com.planora.backend.integration.github;

import com.planora.backend.integration.AbstractIntegrationTest;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end against a real GitHub homologation repository.
 * Configure the following environment variables to enable:
 *   GITHUB_HOMOLOG_TOKEN  - PAT with `repo` scope on the homolog repo
 *   GITHUB_HOMOLOG_OWNER  - owner/org of the homolog repo (e.g. planora-org)
 *   GITHUB_HOMOLOG_REPO   - repository name (e.g. planora-homolog)
 * Each test creates an issue then closes it in @AfterEach to keep the repo tidy.
 */
@Tag("github-live")
@DisplayName("GitHub Live Integration — Backend <-> GitHub homologation repo")
@EnabledIfEnvironmentVariable(named = "GITHUB_HOMOLOG_TOKEN", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GITHUB_HOMOLOG_OWNER", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GITHUB_HOMOLOG_REPO", matches = ".+")
class GithubLiveIntegrationTest extends AbstractIntegrationTest {

    private static final String HOMOLOG_TOKEN = System.getenv("GITHUB_HOMOLOG_TOKEN");
    private static final String HOMOLOG_OWNER = System.getenv("GITHUB_HOMOLOG_OWNER");
    private static final String HOMOLOG_REPO = System.getenv("GITHUB_HOMOLOG_REPO");

    private final WebClient github = WebClient.builder()
            .baseUrl("https://api.github.com")
            .defaultHeader("Authorization", "Bearer " + HOMOLOG_TOKEN)
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();

    private Integer createdIssueNumber;

    @AfterEach
    void closeCreatedIssue() {
        if (createdIssueNumber == null) return;
        try {
            github.patch()
                    .uri("/repos/{owner}/{repo}/issues/{n}", HOMOLOG_OWNER, HOMOLOG_REPO, createdIssueNumber)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"state\":\"closed\"}")
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } finally {
            createdIssueNumber = null;
        }
    }

    @Test
    @DisplayName("createIssue against real homolog repo: title, Markdown body and labels are honored by GitHub")
    void createIssue_againstHomologRepo_titleBodyLabelsRoundTrip() throws Exception {
        User owner = persistUser(HOMOLOG_OWNER, HOMOLOG_TOKEN);
        KanbanBoard board = persistBoardWithColumns(owner, HOMOLOG_OWNER, HOMOLOG_REPO);
        KanbanColumn todo = kanbanColumnRepository
                .findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(board.getKanbanBoardId())
                .stream().filter(c -> c.getPosition() == 0).findFirst().orElseThrow();

        String title = "[integration-test] " + java.time.Instant.now();
        String markdownBody = "## Auto-generated\n\nThis issue was created by `GithubLiveIntegrationTest`.\n\n- [x] First\n- [ ] Second";

        IssueRequest request = new IssueRequest(title, markdownBody, List.of(), List.of());

        mockMvc.perform(post("/v1/kanban/board/issue/create")
                        .with(jwtFor(owner))
                        .param("boardId", board.getKanbanBoardId().toString())
                        .param("columnId", todo.getKanbanColumnId().toString())
                        .param("repository", HOMOLOG_REPO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated());

        List<Issue> persisted = issueRepository.findAll();
        assertThat(persisted).hasSize(1);
        createdIssueNumber = persisted.get(0).getNumber();
        assertThat(createdIssueNumber).isNotNull();

        // Round-trip: read back from GitHub and verify what we persisted matches what GitHub saved.
        JsonNode fetched = github.get()
                .uri("/repos/{owner}/{repo}/issues/{n}", HOMOLOG_OWNER, HOMOLOG_REPO, createdIssueNumber)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        assertThat(fetched).isNotNull();
        assertThat(fetched.get("title").asString()).isEqualTo(title);
        assertThat(fetched.get("body").asString()).isEqualTo(markdownBody);
        assertThat(fetched.get("state").asString()).isEqualTo("open");
    }
}
