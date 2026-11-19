package ifmo.poster.user.service;

import ifmo.poster.user.dto.LoginRequest;
import ifmo.poster.user.dto.TokenResponse;
import ifmo.poster.user.exception.AuthenticationFailedException;
import ifmo.poster.user.security.PosterUserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class AuthService {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public Mono<TokenResponse> login(LoginRequest request) {
        return Mono.fromCallable(() -> authenticate(request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private TokenResponse authenticate(LoginRequest request) {
        PosterUserDetails userDetails;
        try {
            userDetails = (PosterUserDetails) userDetailsService.loadUserByUsername(request.userName());
        } catch (UsernameNotFoundException ex) {
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
            throw invalidCredentials();
        }
        return jwtTokenService.issue(userDetails.getDomainUser());
    }

    private AuthenticationFailedException invalidCredentials() {
        return new AuthenticationFailedException("Неверное имя пользователя или пароль");
    }
}
