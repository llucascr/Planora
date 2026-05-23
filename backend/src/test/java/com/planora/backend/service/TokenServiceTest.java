package com.planora.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService")
class TokenServiceTest {

    private static final String GITHUB_TOKEN = "github-pat-123";

    @Mock private Jwt jwt;

    @InjectMocks private TokenService tokenService;

    @Nested
    @DisplayName("getUserId")
    class GetUserId {

        @Test
        @DisplayName("deve converter subject do JWT para Long quando subject é numérico")
        void deveConverterSubjectParaLong_quandoSubjectEhNumerico() {
            when(jwt.getSubject()).thenReturn("42");

            Long userId = tokenService.getUserId(jwt);

            assertThat(userId).isEqualTo(42L);
        }

        @Test
        @DisplayName("deve lançar NumberFormatException quando subject não é numérico")
        void deveLancarNumberFormatException_quandoSubjectNaoEhNumerico() {
            when(jwt.getSubject()).thenReturn("not-a-number");

            assertThatThrownBy(() -> tokenService.getUserId(jwt))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("deve lançar NumberFormatException quando subject é null")
        void deveLancarNumberFormatException_quandoSubjectEhNull() {
            when(jwt.getSubject()).thenReturn(null);

            assertThatThrownBy(() -> tokenService.getUserId(jwt))
                    .isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("getGithubToken")
    class GetGithubToken {

        @Test
        @DisplayName("deve retornar valor da claim githubToken quando presente")
        void deveRetornarValorDaClaim_quandoClaimPresente() {
            when(jwt.getClaimAsString("githubToken")).thenReturn(GITHUB_TOKEN);

            String result = tokenService.getGithubToken(jwt);

            assertThat(result).isEqualTo(GITHUB_TOKEN);
        }

        @Test
        @DisplayName("deve retornar null quando claim githubToken não existe")
        void deveRetornarNull_quandoClaimNaoExiste() {
            when(jwt.getClaimAsString("githubToken")).thenReturn(null);

            String result = tokenService.getGithubToken(jwt);

            assertThat(result).isNull();
        }
    }
}
