package ifmo.poster.user.controller;

import ifmo.poster.contracts.support.Pagination;
import ifmo.poster.user.dto.CreateNotificationRequest;
import ifmo.poster.user.dto.NotificationResponse;
import ifmo.poster.user.dto.UpdateNotificationRequest;
import ifmo.poster.user.service.NotificationService;
import ifmo.poster.security.AuthenticatedUser;
import ifmo.poster.security.JwtSupport;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPERVISOR')")
    public Mono<NotificationResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
        return notificationService.create(request);
    }

    @GetMapping("/{id}")
    public Mono<NotificationResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return notificationService.getById(id)
                .map(body -> requireOwnerOrSupervisor(body, JwtSupport.authenticatedUser(jwt)));
    }

    @GetMapping
    public Mono<List<NotificationResponse>> findAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE_SIZE) int size,
            ServerHttpResponse response,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUser actor = JwtSupport.authenticatedUser(jwt);
        Long effectiveUserId = actor.isSupervisor() ? userId : actor.id();
        return notificationService.findAll(effectiveUserId, page, size)
                .doOnNext(result -> response.getHeaders()
                        .add(Pagination.TOTAL_COUNT_HEADER, String.valueOf(result.getTotalElements())))
                .map(Page::getContent);
    }

    @PutMapping("/{id}")
    public Mono<NotificationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNotificationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUser actor = JwtSupport.authenticatedUser(jwt);
        return notificationService.getById(id)
                .map(body -> requireOwnerOrSupervisor(body, actor))
                .flatMap(ignored -> notificationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPERVISOR')")
    public Mono<Void> delete(@PathVariable Long id) {
        return notificationService.delete(id);
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
