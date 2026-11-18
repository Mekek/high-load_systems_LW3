package ifmo.poster.user.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("userAccess")
public class UserAccess {

    public boolean owns(Long userId, Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return false;
        }
        Number tokenUserId = jwtAuthentication.getToken().getClaim("userId");
        return tokenUserId != null && userId != null && tokenUserId.longValue() == userId.longValue();
    }
}
