package ifmo.poster.event.controller;

import ifmo.poster.event.dto.CreateWaitlistRequest;
import ifmo.poster.event.dto.UpdateWaitlistRequest;
import ifmo.poster.event.dto.WaitlistResponse;
import ifmo.poster.event.service.WaitlistService;
import ifmo.poster.contracts.support.Pagination;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
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
import ifmo.poster.security.AuthenticatedUser;
import ifmo.poster.security.JwtSupport;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/ticket-waitlist")
public class WaitlistController {
    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping
    public Mono<ResponseEntity<WaitlistResponse>> create(
            @Valid @RequestBody CreateWaitlistRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        requireOwnerOrSupervisor(request.userId(), JwtSupport.authenticatedUser(jwt));
        return waitlistService.create(request)
                .flatMap(body -> Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(body)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<WaitlistResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return waitlistService.getById(id)
                .map(body -> requireOwnerOrSupervisor(body, JwtSupport.authenticatedUser(jwt)))
                .flatMap(body -> Mono.just(ResponseEntity.ok(body)));
    }

    @GetMapping
    public Mono<ResponseEntity<List<WaitlistResponse>>> findAll(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE_SIZE) int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUser actor = JwtSupport.authenticatedUser(jwt);
        Long effectiveUserId = actor.isSupervisor() ? userId : actor.id();
        return waitlistService.findAll(eventId, effectiveUserId, page, size)
                .flatMap(tuple -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(Pagination.TOTAL_COUNT_HEADER, String.valueOf(tuple.getT2()));
                    return Mono.just(ResponseEntity.ok().headers(headers).body(tuple.getT1()));
                });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<WaitlistResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWaitlistRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUser actor = JwtSupport.authenticatedUser(jwt);
        return waitlistService.getById(id)
                .map(body -> requireOwnerOrSupervisor(body, actor))
                .flatMap(ignored -> waitlistService.update(id, request))
                .flatMap(body -> Mono.just(ResponseEntity.ok(body)));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUser actor = JwtSupport.authenticatedUser(jwt);
        return waitlistService.getById(id)
                .map(body -> requireOwnerOrSupervisor(body, actor))
                .flatMap(ignored -> waitlistService.delete(id))
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    private WaitlistResponse requireOwnerOrSupervisor(WaitlistResponse response, AuthenticatedUser actor) {
        requireOwnerOrSupervisor(response.userId(), actor);
        return response;
    }

    private void requireOwnerOrSupervisor(Long userId, AuthenticatedUser actor) {
        if (!actor.isSupervisor() && !actor.owns(userId)) {
            throw new AccessDeniedException("Нельзя управлять чужим листом ожидания");
        }
    }
}
