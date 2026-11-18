package ifmo.poster.user.service;

import ifmo.poster.user.dto.LoginRequest;
import ifmo.poster.user.dto.TokenResponse;
import ifmo.poster.user.entity.User;
import ifmo.poster.user.exception.AuthenticationFailedException;
import ifmo.poster.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public Mono<TokenResponse> login(LoginRequest request) {
        return Mono.fromCallable(() -> authenticate(request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private TokenResponse authenticate(LoginRequest request) {
        User user = userRepository.findByUserName(request.userName())
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return jwtTokenService.issue(user);
    }

    private AuthenticationFailedException invalidCredentials() {
        return new AuthenticationFailedException("Неверное имя пользователя или пароль");
    }
}
