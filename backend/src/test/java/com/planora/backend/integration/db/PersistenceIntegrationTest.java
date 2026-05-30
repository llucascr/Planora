package com.planora.backend.integration.db;

import com.planora.backend.client.GithubIssueClient;
import com.planora.backend.client.GithubLabelClient;
import com.planora.backend.client.GithubRepositoryClient;
import com.planora.backend.client.GithubSearchClient;
import com.planora.backend.client.GithubWebhookClient;
import com.planora.backend.integration.AbstractIntegrationTest;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.State;
import com.planora.backend.model.issue.dto.IssueApiResponse;
import com.planora.backend.model.issue.dto.IssueRequest;
import com.planora.backend.model.issue.dto.MoveIssueRequest;
import com.planora.backend.model.issue.dto.RepositoryResponse;
import com.planora.backend.model.issue.dto.UserIssueResponse;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.kanban.KanbanMember;
import com.planora.backend.model.kanban.dto.InvitedStatus;
import com.planora.backend.model.kanban.dto.KanbanBoardRequest;
import com.planora.backend.model.kanban.dto.KanbanBoardResponse;
import com.planora.backend.model.kanban.dto.MemberInviteRequest;
import com.planora.backend.model.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Persistence Integration — Backend <-> DB")
class PersistenceIntegrationTest extends AbstractIntegrationTest {

    private static final String OWNER = "llucascr";
    private static final String REPO = "planora";

    @MockitoBean private GithubIssueClient githubIssueClient;
    @MockitoBean private GithubRepositoryClient githubRepositoryClient;
    @MockitoBean private GithubLabelClient githubLabelClient;
    @MockitoBean private GithubWebhookClient githubWebhookClient;
    @MockitoBean private GithubSearchClient githubSearchClient;

    @Test
    @DisplayName("POST /board/create persists board with owner as ACCEPTED member and 3 default columns")
    void createBoard_persistsWithDefaultColumnsAndOwnerAsMember() throws Exception {
        User owner = persistUser(OWNER, "ghp_owner_token");
        when(githubRepositoryClient.getRepository(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new RepositoryResponse("1", REPO, OWNER + "/" + REPO, "false"));

        KanbanBoardRequest request = new KanbanBoardRequest("Planora", "desc", OWNER + "/" + REPO, List.of());

        // The HTTP response returns columns:[] because KanbanBoardService.getBoardById reads
        // the just-saved board from Hibernate's first-level cache, which doesn't reflect the
        // default columns inserted right after. We verify persistence via the repositories below.
        mockMvc.perform(post("/v1/kanban/board/create")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Planora"))
                .andExpect(jsonPath("$.githubRepository").value(REPO))
                .andExpect(jsonPath("$.githubOwnerName").value(OWNER))
                .andExpect(jsonPath("$.members.length()").value(1));

        List<KanbanBoard> boards = kanbanBoardRepository.findAll();
        assertThat(boards).hasSize(1);
        KanbanBoard board = boards.get(0);
        assertThat(board.getName()).isEqualTo("Planora");
        assertThat(board.getDescription()).isEqualTo("desc");
        assertThat(board.getGithubOwnerName()).isEqualTo(OWNER);
        assertThat(board.getGithubRepository()).isEqualTo(REPO);
        assertThat(board.getOwner().getUserId()).isEqualTo(owner.getUserId());

        List<KanbanColumn> columns = kanbanColumnRepository.findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(board.getKanbanBoardId());
        assertThat(columns).extracting(KanbanColumn::getName).containsExactly("Todo", "In Progress", "Done");
        assertThat(columns).extracting(KanbanColumn::getPosition).containsExactly(0, 1, 2);

        List<KanbanMember> members = kanbanMemberRepository.findByKanbanBoard_KanbanBoardId(board.getKanbanBoardId());
        assertThat(members).hasSize(1);
        assertThat(members.get(0).getUser().getUserId()).isEqualTo(owner.getUserId());
        assertThat(members.get(0).getInvitedStatus()).isEqualTo(InvitedStatus.ACCEPTED);
    }

    @Test
    @DisplayName("POST /board/create rejects invalid githubRepository format (no slash) without persisting")
    void createBoard_rejectsInvalidRepoFormat() throws Exception {
        User owner = persistUser(OWNER, "ghp_owner_token");
        KanbanBoardRequest request = new KanbanBoardRequest("Planora", "desc", "no-slash", List.of());

        // IllegalArgumentException falls through to handleAllException -> 500.
        // This documents the current behavior; refining the mapping would be a follow-up.
        mockMvc.perform(post("/v1/kanban/board/create")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().is5xxServerError());

        assertThat(kanbanBoardRepository.count()).isZero();
    }

    @Test
    @DisplayName("GET /board/list returns only boards where user is ACCEPTED member")
    void listBoards_filtersByAcceptedMembership() throws Exception {
        User userA = persistUser("alice", "tok-a");
        User userB = persistUser("bob", "tok-b");
        KanbanBoard boardA = persistBoardWithColumns(userA, OWNER, "repo-a");
        KanbanBoard boardB = persistBoardWithColumns(userB, OWNER, "repo-b");

        KanbanMember pending = KanbanMember.builder()
                .kanbanBoard(boardB)
                .user(userA)
                .invitedStatus(InvitedStatus.PENDING)
                .invitedAt(java.time.LocalDateTime.now())
                .build();
        kanbanMemberRepository.save(pending);

        mockMvc.perform(get("/v1/kanban/board/list").with(jwtFor(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].kanbanBoardId").value(boardA.getKanbanBoardId()));
    }

    @Test
    @DisplayName("DELETE /board/{id} cascades to columns and members")
    void deleteBoard_cascadesColumnsAndMembers() throws Exception {
        User owner = persistUser(OWNER, "tok");
        KanbanBoard board = persistBoardWithColumns(owner, OWNER, REPO);
        Long boardId = board.getKanbanBoardId();

        mockMvc.perform(delete("/v1/kanban/board/delete/" + boardId).with(jwtFor(owner)))
                .andExpect(status().isOk());

        assertThat(kanbanBoardRepository.findById(boardId)).isEmpty();
        assertThat(kanbanColumnRepository.findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(boardId)).isEmpty();
        assertThat(kanbanMemberRepository.findByKanbanBoard_KanbanBoardId(boardId)).isEmpty();
    }

    @Test
    @DisplayName("POST /board/issue/create persists issue linked to column with title/body/number")
    void createIssue_persistsAndLinksToColumn() throws Exception {
        User owner = persistUser(OWNER, "tok");
        KanbanBoard board = persistBoardWithColumns(owner, OWNER, REPO);
        KanbanColumn todo = kanbanColumnRepository
                .findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(board.getKanbanBoardId())
                .stream().filter(c -> c.getName().equals("Todo")).findFirst().orElseThrow();

        IssueApiResponse apiResponse = new IssueApiResponse(
                "https://github.com/" + OWNER + "/" + REPO + "/issues/1",
                1,
                "Implement login",
                "## Steps\n\n1. Add OAuth2 flow",
                "open",
                new UserIssueResponse(OWNER, "", "", ""),
                List.of(),
                List.of()
        );
        when(githubIssueClient.createIssue(anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(apiResponse);

        IssueRequest request = new IssueRequest("Implement login", "## Steps\n\n1. Add OAuth2 flow", List.of(), List.of());

        mockMvc.perform(post("/v1/kanban/board/issue/create")
                        .with(jwtFor(owner))
                        .param("boardId", board.getKanbanBoardId().toString())
                        .param("columnId", todo.getKanbanColumnId().toString())
                        .param("repository", REPO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated());

        List<Issue> issues = issueRepository.findAll();
        assertThat(issues).hasSize(1);
        Issue persisted = issues.get(0);
        assertThat(persisted.getTitle()).isEqualTo("Implement login");
        assertThat(persisted.getBody()).isEqualTo("## Steps\n\n1. Add OAuth2 flow");
        assertThat(persisted.getNumber()).isEqualTo(1);
        assertThat(persisted.getState()).isEqualTo(State.OPEN);
        assertThat(persisted.getColumn().getKanbanColumnId()).isEqualTo(todo.getKanbanColumnId());
        assertThat(persisted.getUser().getUserId()).isEqualTo(owner.getUserId());
    }

    @Test
    @DisplayName("PATCH /board/{boardId}/issue/move updates issue.column to target column")
    void moveIssue_updatesColumnReference() throws Exception {
        User owner = persistUser(OWNER, "tok");
        KanbanBoard board = persistBoardWithColumns(owner, OWNER, REPO);
        List<KanbanColumn> columns = kanbanColumnRepository
                .findByKanbanBoard_KanbanBoardIdOrderByPositionAsc(board.getKanbanBoardId());
        KanbanColumn todo = columns.stream().filter(c -> c.getPosition() == 0).findFirst().orElseThrow();
        KanbanColumn inProgress = columns.stream().filter(c -> c.getPosition() == 1).findFirst().orElseThrow();
        Issue issue = persistIssue(todo, owner, "Move me", "body", 42);

        MoveIssueRequest request = new MoveIssueRequest(issue.getIssueId(), inProgress.getKanbanColumnId());

        mockMvc.perform(patch("/v1/kanban/board/" + board.getKanbanBoardId() + "/issue/move")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk());

        Issue moved = issueRepository.findById(issue.getIssueId()).orElseThrow();
        assertThat(moved.getColumn().getKanbanColumnId()).isEqualTo(inProgress.getKanbanColumnId());
    }

    @Test
    @DisplayName("POST /board/{id}/member/invite persists KanbanMember with PENDING status")
    void inviteMember_persistsWithPending() throws Exception {
        User owner = persistUser(OWNER, "tok");
        User invitee = persistUser("bob", "tok-bob");
        KanbanBoard board = persistBoardWithColumns(owner, OWNER, REPO);

        MemberInviteRequest request = new MemberInviteRequest("bob");

        mockMvc.perform(post("/v1/kanban/board/" + board.getKanbanBoardId() + "/member/invite")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.login").value("bob"))
                .andExpect(jsonPath("$.invitedStatus").value("PENDING"));

        Optional<KanbanMember> persisted = kanbanMemberRepository
                .findByKanbanBoard_KanbanBoardIdAndUser_UserId(board.getKanbanBoardId(), invitee.getUserId());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getInvitedStatus()).isEqualTo(InvitedStatus.PENDING);
        assertThat(persisted.get().getJoinedAt()).isNull();
    }

    @Test
    @DisplayName("PATCH /member/{id}/status/update transitions PENDING -> ACCEPTED and sets joinedAt")
    void updateMemberStatus_acceptInviteSetsJoinedAt() throws Exception {
        User owner = persistUser(OWNER, "tok");
        User invitee = persistUser("bob", "tok-bob");
        KanbanBoard board = persistBoardWithColumns(owner, OWNER, REPO);
        KanbanMember pending = KanbanMember.builder()
                .kanbanBoard(board)
                .user(invitee)
                .invitedStatus(InvitedStatus.PENDING)
                .invitedAt(java.time.LocalDateTime.now())
                .build();
        pending = kanbanMemberRepository.save(pending);

        mockMvc.perform(patch("/v1/kanban/member/" + pending.getKanbanMemberId() + "/status/update")
                        .with(jwtFor(invitee))
                        .param("status", "ACCEPTED"))
                .andExpect(status().isOk());

        KanbanMember refreshed = kanbanMemberRepository.findById(pending.getKanbanMemberId()).orElseThrow();
        assertThat(refreshed.getInvitedStatus()).isEqualTo(InvitedStatus.ACCEPTED);
        assertThat(refreshed.getJoinedAt()).isNotNull();
    }

    @Test
    @DisplayName("POST /member/invite twice for same user returns 409 (DataAlreadyExistException)")
    void duplicateInvite_returns409() throws Exception {
        User owner = persistUser(OWNER, "tok");
        persistUser("bob", "tok-bob");
        KanbanBoard board = persistBoardWithColumns(owner, OWNER, REPO);

        MemberInviteRequest request = new MemberInviteRequest("bob");

        mockMvc.perform(post("/v1/kanban/board/" + board.getKanbanBoardId() + "/member/invite")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/kanban/board/" + board.getKanbanBoardId() + "/member/invite")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isConflict());
    }
}
