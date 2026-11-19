package ifmo.poster.user.controller;

import ifmo.poster.contracts.support.Pagination;
import ifmo.poster.user.dto.BulkCreateUsersRequest;
import ifmo.poster.user.dto.CreateUserRequest;
import ifmo.poster.user.dto.UpdateUserRequest;
import ifmo.poster.user.dto.UserResponse;
import ifmo.poster.user.service.UserService;
import ifmo.poster.security.JwtSupport;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPERVISOR')")
    public Mono<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPERVISOR')")
    public Mono<List<UserResponse>> createBulk(@Valid @RequestBody BulkCreateUsersRequest request) {
        return userService.createBulk(request.users());
    }

    @DeleteMapping("/data")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPERVISOR')")
    public Mono<Void> clearAllData() {
        return userService.clearAllData();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPERVISOR') or @userAccess.owns(#id, authentication)")
    public Mono<UserResponse> getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPERVISOR')")
    public Mono<List<UserResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE_SIZE) int size,
            ServerHttpResponse response
    ) {
        return userService.findAll(page, size)
                .doOnNext(result -> response.getHeaders()
                        .add(Pagination.TOTAL_COUNT_HEADER, String.valueOf(result.getTotalElements())))
                .map(Page::getContent);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPERVISOR') or @userAccess.owns(#id, authentication)")
    public Mono<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return userService.update(id, request, JwtSupport.authenticatedUser(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPERVISOR')")
    public Mono<Void> delete(@PathVariable Long id) {
        return userService.delete(id);
    }
}
