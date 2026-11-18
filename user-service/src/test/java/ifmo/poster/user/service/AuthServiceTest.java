package ifmo.poster.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ifmo.poster.contracts.enums.Role;
import ifmo.poster.user.dto.LoginRequest;
import ifmo.poster.user.dto.TokenResponse;
import ifmo.poster.user.entity.User;
import ifmo.poster.user.exception.AuthenticationFailedException;
import ifmo.poster.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    JwtTokenService jwtTokenService;

    PasswordEncoder passwordEncoder;
    AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenService);
    }

    @Test
    void acceptsCorrectPasswordAndReturnsToken() {
        User user = user("alice", "Correct123!");
        TokenResponse expected = new TokenResponse("jwt", "Bearer", 3600);
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(user));
        when(jwtTokenService.issue(user)).thenReturn(expected);

        TokenResponse actual = authService.login(new LoginRequest("alice", "Correct123!")).block();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void rejectsWrongPasswordWithoutExplainingWhichFieldIsWrong() {
        when(userRepository.findByUserName("alice"))
                .thenReturn(Optional.of(user("alice", "Correct123!")));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")).block())
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Неверное имя пользователя или пароль");
    }

    @Test
    void rejectsUnknownUserWithSamePublicMessage() {
        when(userRepository.findByUserName("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing", "wrong")).block())
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Неверное имя пользователя или пароль");
    }

    private User user(String userName, String rawPassword) {
        User user = new User();
        user.setId(1L);
        user.setUserName(userName);
        user.setRole(Role.USER);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return user;
    }
}
