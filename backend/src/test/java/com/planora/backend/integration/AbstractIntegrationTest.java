package com.planora.backend.integration;

import tools.jackson.databind.ObjectMapper;
import com.planora.backend.model.issue.Issue;
import com.planora.backend.model.issue.State;
import com.planora.backend.model.kanban.KanbanBoard;
import com.planora.backend.model.kanban.KanbanColumn;
import com.planora.backend.model.kanban.KanbanMember;
import com.planora.backend.model.kanban.dto.InvitedStatus;
import com.planora.backend.model.user.User;
import com.planora.backend.repository.IssueRepository;
import com.planora.backend.repository.JobRepository;
import com.planora.backend.repository.KanbanBoardRepository;
import com.planora.backend.repository.KanbanColumnRepository;
import com.planora.backend.repository.KanbanMemberRepository;
import com.planora.backend.repository.LabelRepository;
import com.planora.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    @Autowired protected UserRepository userRepository;
    @Autowired protected KanbanBoardRepository kanbanBoardRepository;
    @Autowired protected KanbanColumnRepository kanbanColumnRepository;
    @Autowired protected KanbanMemberRepository kanbanMemberRepository;
    @Autowired protected IssueRepository issueRepository;
    @Autowired protected LabelRepository labelRepository;
    @Autowired protected JobRepository jobRepository;

    // Stops AdminUserConfig CommandLineRunner from polluting test state.
    @MockitoBean protected com.planora.backend.config.AdminUserConfig adminUserConfig;

    @BeforeEach
    void cleanDatabase() {
        issueRepository.deleteAll();
        kanbanMemberRepository.deleteAll();
        kanbanColumnRepository.deleteAll();
        kanbanBoardRepository.deleteAll();
        jobRepository.deleteAll();
        labelRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User persistUser(String login, String githubToken) {
        LocalDateTime now = LocalDateTime.now();
        return userRepository.save(User.builder()
                .login(login)
                .githubToken(githubToken)
                .email(login + "@test.local")
                .notificationEmail(login + "@test.local")
                .avatarUrl("https://avatars.example.com/" + login)
                .profileUrl("https://github.com/" + login)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    protected KanbanBoard persistBoardWithColumns(User owner, String ownerName, String repository) {
        KanbanBoard board = KanbanBoard.builder()
                .name(repository + " board")
                .description("Integration test board")
                .githubOwnerName(ownerName)
                .githubRepository(repository)
                .owner(owner)
                .members(new ArrayList<>())
                .columns(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        KanbanMember ownerMember = KanbanMember.builder()
                .kanbanBoard(board)
                .user(owner)
                .invitedAt(LocalDateTime.now())
                .joinedAt(LocalDateTime.now())
                .invitedStatus(InvitedStatus.ACCEPTED)
                .build();
        board.getMembers().add(ownerMember);

        KanbanBoard saved = kanbanBoardRepository.save(board);
        kanbanColumnRepository.saveAll(List.of(
                buildColumn("Todo", 0, saved),
                buildColumn("In Progress", 1, saved),
                buildColumn("Done", 2, saved)
        ));
        return kanbanBoardRepository.findById(saved.getKanbanBoardId()).orElseThrow();
    }

    protected Issue persistIssue(KanbanColumn column, User author, String title, String body, int number) {
        LocalDateTime now = LocalDateTime.now();
        Issue issue = new Issue();
        issue.setTitle(title);
        issue.setBody(body);
        issue.setNumber(number);
        issue.setUrl("https://github.com/example/repo/issues/" + number);
        issue.setState(State.OPEN);
        issue.setUser(author);
        issue.setLabels(new ArrayList<>());
        issue.setAssignees(new ArrayList<>());
        issue.setColumn(column);
        issue.setCreatedAt(now);
        issue.setUpdatedAt(now);
        return issueRepository.save(issue);
    }

    protected RequestPostProcessor jwtFor(User user) {
        return jwt().jwt(builder -> builder
                .subject(user.getUserId().toString())
                .claim("githubToken", user.getGithubToken())
                .claim("scope", "user")
        );
    }

    protected String json(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    private static KanbanColumn buildColumn(String name, int position, KanbanBoard board) {
        return KanbanColumn.builder()
                .name(name)
                .position(position)
                .kanbanBoard(board)
                .issues(new ArrayList<>())
                .build();
    }
}
