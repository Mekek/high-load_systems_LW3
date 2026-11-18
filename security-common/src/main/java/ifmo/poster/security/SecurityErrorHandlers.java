package ifmo.poster.security;

import java.nio.charset.StandardCharsets;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import reactor.core.publisher.Mono;

public final class SecurityErrorHandlers {

    private SecurityErrorHandlers() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, error) -> writeJson(
                exchange.getResponse(),
                HttpStatus.UNAUTHORIZED,
                "Требуется действующий JWT-токен"
        );
    }

    public static ServerAccessDeniedHandler accessDeniedHandler() {
        return (exchange, error) -> writeJson(
                exchange.getResponse(),
                HttpStatus.FORBIDDEN,
                "Недостаточно прав для выполнения операции"
        );
    }

    private static Mono<Void> writeJson(
            org.springframework.http.server.reactive.ServerHttpResponse response,
            HttpStatus status,
            String message
    ) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"status\":" + status.value()
                + ",\"error\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
