package ifmo.poster.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String userName,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank String password
) {
}
