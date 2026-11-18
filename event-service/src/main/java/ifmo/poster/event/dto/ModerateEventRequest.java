package ifmo.poster.event.dto;

import ifmo.poster.contracts.enums.EventStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModerateEventRequest(
        @NotNull EventStatus status,
        @Size(max = 500) String blockReason
) {
}
