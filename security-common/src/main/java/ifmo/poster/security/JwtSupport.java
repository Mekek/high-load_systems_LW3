package ifmo.poster.security;

import ifmo.poster.contracts.enums.Role;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.Instant;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

public final class JwtSupport {

    private JwtSupport() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static ReactiveJwtDecoder decoder(String secret) {
        SecretKey key = secretKey(secret);
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    public static SecretKey secretKey(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET должен содержать не менее 32 байт");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public static Converter<Jwt, Mono<AbstractAuthenticationToken>> authenticationConverter() {
        return jwt -> {
            Role role = Role.valueOf(jwt.getClaimAsString("role"));
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            return Mono.just(new JwtAuthenticationToken(jwt, authorities, jwt.getSubject()));
        };
    }

    public static AuthenticatedUser authenticatedUser(Jwt jwt) {
        Number rawId = jwt.getClaim("userId");
        Long userId = rawId == null ? null : rawId.longValue();
        return new AuthenticatedUser(
                userId,
                jwt.getSubject(),
                Role.valueOf(jwt.getClaimAsString("role"))
        );
    }

    public static String encode(
            String secret,
            AuthenticatedUser user,
            Duration lifetime
    ) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("poster-user-service")
                .issuedAt(now)
                .expiresAt(now.plus(lifetime))
                .subject(user.userName())
                .claim("userId", user.id())
                .claim("role", user.role().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(secret)));
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
