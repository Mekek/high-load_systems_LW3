package ifmo.poster.event.controller;

import ifmo.poster.contracts.enums.EventStatus;
import ifmo.poster.event.dto.BulkCreateEventsRequest;
import ifmo.poster.event.dto.CreateEventRequest;
import ifmo.poster.event.dto.EventResponse;
import ifmo.poster.event.dto.UpdateEventRequest;
import ifmo.poster.event.dto.ModerateEventRequest;
import ifmo.poster.event.service.EventService;
import ifmo.poster.contracts.support.Pagination;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERUSER', 'SUPERVISOR')")
    public Mono<ResponseEntity<EventResponse>> create(@Valid @RequestBody CreateEventRequest request) {
        return eventService.create(request)
                .flatMap(body -> Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(body)));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('SUPERUSER', 'SUPERVISOR')")
    public Mono<ResponseEntity<List<EventResponse>>> createBulk(
            @Valid @RequestBody BulkCreateEventsRequest request
    ) {
        return eventService.createBulk(request.events())
                .flatMap(body -> Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(body)));
    }

    @DeleteMapping("/data")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public Mono<ResponseEntity<Void>> clearAllData() {
        return eventService.clearAllData()
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<EventResponse>> getById(@PathVariable Long id) {
        return eventService.getById(id)
                .flatMap(body -> Mono.just(ResponseEntity.ok(body)));
    }

    /**
     * Infinite scroll without total count: cursor by {@code afterId}.
     */
    @GetMapping
    public Mono<ResponseEntity<List<EventResponse>>> findAll(
            @RequestParam(defaultValue = "0") Long afterId,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE_SIZE) int limit,
            @RequestParam(required = false) EventStatus status
    ) {
        return eventService.findNextBatch(afterId, limit, status)
                .flatMap(body -> Mono.just(ResponseEntity.ok(body)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERUSER', 'SUPERVISOR')")
    public Mono<ResponseEntity<EventResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        return eventService.update(id, request)
                .flatMap(body -> Mono.just(ResponseEntity.ok(body)));
    }

    @PutMapping("/{id}/moderation")
    @PreAuthorize("hasAnyRole('CENSOR', 'SUPERVISOR')")
    public Mono<ResponseEntity<EventResponse>> moderate(
            @PathVariable Long id,
            @Valid @RequestBody ModerateEventRequest request
    ) {
        return eventService.moderate(id, request).map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERUSER', 'SUPERVISOR')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return eventService.delete(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
