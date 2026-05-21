package com.planora.backend.service;

import com.planora.backend.exception.DataNotFoundException;
import com.planora.backend.model.user.Role;
import com.planora.backend.model.user.User;
import com.planora.backend.model.user.dto.LoginResponse;
import com.planora.backend.repository.RoleRepository;
import com.planora.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OauthService")
class OauthServiceTest {

    private static final String JWT_TOKEN_VALUE = "encoded-jwt-token";
    private static final String GITHUB_TOKEN = "github-pat-123";
    private static final String NEW_GITHUB_TOKEN = "new-github-pat-456";
    private static final String LOGIN = "llucascr";
    private static final String AVATAR_URL = "https://avatars.example.com/u/42";
    private static final String PROFILE_URL = "https://github.com/llucascr";
    private static final String EMAIL = "user@example.com";
    private static final String NOTIFICATION_EMAIL = "notify@example.com";
    private static final String BASIC_ROLE_NAME = "user";
    private static final Long USER_ID = 42L;

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private JwtEncoder jwtEncoder;
    @Mock private OAuth2User oAuth2User;

    @InjectMocks private OauthService oauthService;

    private Role basicRole;

    @BeforeEach
    void setUp() {
        basicRole = buildRole(1L, BASIC_ROLE_NAME);
    }

    private Role buildRole(Long id, String name) {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "roleId", id);
        ReflectionTestUtils.setField(role, "name", name);
        return role;
    }

    private User buildExistingUser() {
        return User.builder()
                .userId(USER_ID)
                .login(LOGIN)
                .githubToken(GITHUB_TOKEN)
                .roles(Set.of(basicRole))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Jwt buildEncodedJwt() {
        Instant now = Instant.now();
        return Jwt.withTokenValue(JWT_TOKEN_VALUE)
                .header("alg", "RS256")
                .issuer("Planora")
                .subject(String.valueOf(USER_ID))
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofDays(1)))
                .build();
    }

    private void stubAttributes(String login, String avatarUrl, String profileUrl, String email, String notificationEmail) {
        when(oAuth2User.<String>getAttribute("login")).thenReturn(login);
        when(oAuth2User.<String>getAttribute("avatar_url")).thenReturn(avatarUrl);
        when(oAuth2User.<String>getAttribute("url")).thenReturn(profileUrl);
        when(oAuth2User.<String>getAttribute("email")).thenReturn(email);
        when(oAuth2User.<String>getAttribute("notification_email")).thenReturn(notificationEmail);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("deve lançar DataNotFoundException quando oAuth2User é null")
        void deveLancarDataNotFoundException_quandoOauth2UserEhNull() {
            assertThatThrownBy(() -> oauthService.save(null, GITHUB_TOKEN))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessageContaining("User not exists");

            verifyNoInteractions(userRepository, roleRepository, jwtEncoder);
        }

        @Test
        @DisplayName("deve atualizar githubToken e gerar JWT quando usuário existe")
        void deveAtualizarGithubTokenEGerarJwt_quandoUsuarioExiste() {
            User existing = buildExistingUser();
            when(oAuth2User.<String>getAttribute("login")).thenReturn(LOGIN);
            when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.of(existing));
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(buildEncodedJwt());

            LoginResponse response = oauthService.save(oAuth2User, NEW_GITHUB_TOKEN);

            assertThat(existing.getGithubToken()).isEqualTo(NEW_GITHUB_TOKEN);
            verify(userRepository).save(existing);
            verify(roleRepository, never()).findByName(any());
            assertThat(response.accessToken()).isEqualTo(JWT_TOKEN_VALUE);
            assertThat(response.expiresIn()).isEqualTo(Duration.ofDays(1).toSeconds());

            JwtClaimsSet claims = captureClaims();
            assertThat(claims.<String>getClaim("iss")).isEqualTo("Planora");
            assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
            assertThat(claims.<String>getClaim("scope")).isEqualTo(BASIC_ROLE_NAME);
            assertThat(claims.<String>getClaim("githubToken")).isEqualTo(NEW_GITHUB_TOKEN);
            Instant issuedAt = claims.getIssuedAt();
            Instant expiresAt = claims.getExpiresAt();
            assertThat(issuedAt).isNotNull();
            assertThat(expiresAt).isNotNull();
            assertThat(Duration.between(issuedAt, expiresAt)).isCloseTo(Duration.ofDays(1), Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("deve criar novo usuário com role BASIC quando usuário não existe")
        void deveCriarNovoUsuarioComRoleBasic_quandoUsuarioNaoExiste() {
            stubAttributes(LOGIN, AVATAR_URL, PROFILE_URL, EMAIL, NOTIFICATION_EMAIL);
            when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.empty());
            when(roleRepository.findByName(BASIC_ROLE_NAME)).thenReturn(basicRole);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setUserId(USER_ID);
                return u;
            });
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenAnswer(invocation -> buildEncodedJwt());

            LoginResponse response = oauthService.save(oAuth2User, GITHUB_TOKEN);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();

            assertThat(saved.getLogin()).isEqualTo(LOGIN);
            assertThat(saved.getAvatarUrl()).isEqualTo(AVATAR_URL);
            assertThat(saved.getProfileUrl()).isEqualTo(PROFILE_URL);
            assertThat(saved.getEmail()).isEqualTo(EMAIL);
            assertThat(saved.getNotificationEmail()).isEqualTo(NOTIFICATION_EMAIL);
            assertThat(saved.getGithubToken()).isEqualTo(GITHUB_TOKEN);
            assertThat(saved.getRoles()).containsExactly(basicRole);
            assertThat(saved.getCreatedAt()).isCloseTo(LocalDateTime.now(), within(1, java.time.temporal.ChronoUnit.SECONDS));
            assertThat(saved.getUpdatedAt()).isCloseTo(LocalDateTime.now(), within(1, java.time.temporal.ChronoUnit.SECONDS));

            assertThat(response.accessToken()).isEqualTo(JWT_TOKEN_VALUE);
            assertThat(response.expiresIn()).isEqualTo(Duration.ofDays(1).toSeconds());
        }
    }

    @Nested
    @DisplayName("generateNonExpiringToken")
    class GenerateNonExpiringToken {

        @Test
        @DisplayName("deve gerar token sem expiração quando usuário existe")
        void deveGerarTokenSemExpiracao_quandoUsuarioExiste() {
            User existing = buildExistingUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(buildEncodedJwt());

            String token = oauthService.generateNonExpiringToken(USER_ID);

            assertThat(token).isEqualTo(JWT_TOKEN_VALUE);

            JwtClaimsSet claims = captureClaims();
            assertThat(claims.<String>getClaim("iss")).isEqualTo("Planora");
            assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiresAt()).isNull();
            assertThat(claims.<String>getClaim("scope")).isEqualTo(BASIC_ROLE_NAME);
            assertThat(claims.<String>getClaim("githubToken")).isEqualTo(GITHUB_TOKEN);
        }

        @Test
        @DisplayName("deve lançar DataNotFoundException quando usuário não existe")
        void deveLancarDataNotFoundException_quandoUsuarioNaoExiste() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oauthService.generateNonExpiringToken(USER_ID))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessageContaining("User not found");

            verifyNoInteractions(jwtEncoder);
        }
    }

    private JwtClaimsSet captureClaims() {
        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        return captor.getValue().getClaims();
    }
}
