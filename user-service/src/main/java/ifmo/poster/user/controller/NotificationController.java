package ifmo.poster.user.controller;

import ifmo.poster.user.dto.CreateNotificationRequest;
import ifmo.poster.user.dto.NotificationResponse;
import ifmo.poster.user.dto.UpdateNotificationRequest;
import ifmo.poster.user.service.NotificationService;
import ifmo.poster.contracts.support.Pagination;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERVISOR')")
    public Mono<ResponseEntity<NotificationResponse>> create(
            @Valid @RequestBody CreateNotificationRequest request
    ) {
        return notificationService.create(request)
                .flatMap(body -> Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(body)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<NotificationResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return notificationService.getById(id)
                .map(body -> requireOwnerOrSupervisor(body, JwtSupport.authenticatedUser(jwt)))
                .flatMap(body -> Mono.just(ResponseEntity.ok(body)));
    }

    @GetMapping
    public Mono<ResponseEntity<List<NotificationResponse>>> findAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE_SIZE) int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUser actor = JwtSupport.authenticatedUser(jwt);
        Long effectiveUserId = actor.isSupervisor() ? userId : actor.id();
        return notificationService.findAll(effectiveUserId, page, size)
                .flatMap(result -> Mono.just(toPagedResponse(result)));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<NotificationResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNotificationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUser actor = JwtSupport.authenticatedUser(jwt);
        return notificationService.getById(id)
                .map(body -> requireOwnerOrSupervisor(body, actor))
                .flatMap(ignored -> notificationService.update(id, request))
                .flatMap(body -> Mono.just(ResponseEntity.ok(body)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return notificationService.delete(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    private ResponseEntity<List<NotificationResponse>> toPagedResponse(Page<NotificationResponse> page) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(Pagination.TOTAL_COUNT_HEADER, String.valueOf(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    private NotificationResponse requireOwnerOrSupervisor(
            NotificationResponse response,
            AuthenticatedUser actor
    ) {
        if (!actor.isSupervisor() && !actor.owns(response.userId())) {
            throw new AccessDeniedException("Нельзя просматривать чужие уведомления");
        }
        return response;
    }
}
