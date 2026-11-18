package ifmo.poster.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SeedPasswordHashTest {

    @Test
    void seedHashMatchesDocumentedPassword() {
        String hash = "$2a$10$zfYynkEajkBv/2rZBnWIse3fHRnRFe8TXCgW6T/AqvwsmRUH9ZsKG";
        assertThat(new BCryptPasswordEncoder().matches("Poster123!", hash)).isTrue();
    }
}
