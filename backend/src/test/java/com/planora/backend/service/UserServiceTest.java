package com.planora.backend.service;

import com.planora.backend.model.user.User;
import com.planora.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    private static final Long USER_ID = 42L;
    private static final String LOGIN = "llucascr";

    @Mock private UserRepository userRepository;

    @InjectMocks private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(USER_ID)
                .login(LOGIN)
                .build();
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("deve retornar usuário quando existe")
        void deveRetornarUsuario_quandoExiste() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            User result = userService.findById(USER_ID);

            assertThat(result).isSameAs(user);
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException quando usuário não existe")
        void deveLancarEntityNotFoundException_quandoUsuarioNaoExiste() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(USER_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found with id: " + USER_ID);
        }
    }

    @Nested
    @DisplayName("findByLogin")
    class FindByLogin {

        @Test
        @DisplayName("deve retornar usuário quando login existe")
        void deveRetornarUsuario_quandoLoginExiste() {
            when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.of(user));

            User result = userService.findByLogin(LOGIN);

            assertThat(result).isSameAs(user);
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException quando login não existe")
        void deveLancarEntityNotFoundException_quandoLoginNaoExiste() {
            when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByLogin(LOGIN))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found: " + LOGIN);
        }
    }

    @Nested
    @DisplayName("findOptionalByLogin")
    class FindOptionalByLogin {

        @Test
        @DisplayName("deve retornar Optional com usuário quando login existe")
        void deveRetornarOptionalComUsuario_quandoLoginExiste() {
            when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.of(user));

            Optional<User> result = userService.findOptionalByLogin(LOGIN);

            assertThat(result).contains(user);
        }

        @Test
        @DisplayName("deve retornar Optional vazio quando login não existe")
        void deveRetornarOptionalVazio_quandoLoginNaoExiste() {
            when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.empty());

            Optional<User> result = userService.findOptionalByLogin(LOGIN);

            assertThat(result).isEmpty();
        }
    }
}
