package ifmo.poster.order.controller;

import ifmo.poster.contracts.support.Pagination;
import ifmo.poster.order.dto.BulkCreateOrdersRequest;
import ifmo.poster.order.dto.CreateOrderRequest;
import ifmo.poster.order.dto.OrderResponse;
import ifmo.poster.order.dto.UpdateOrderStatusRequest;
import ifmo.poster.order.service.OrderService;
import ifmo.poster.security.JwtSupport;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Mono<ResponseEntity<OrderResponse>> create(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.create(request, JwtSupport.authenticatedUser(jwt))
                .flatMap(body -> Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(body)));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public Mono<ResponseEntity<List<OrderResponse>>> createBulk(
            @Valid @RequestBody BulkCreateOrdersRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.createBulk(request.orders(), JwtSupport.authenticatedUser(jwt))
                .flatMap(body -> Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(body)));
    }

    @DeleteMapping("/data")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public Mono<ResponseEntity<Void>> clearAllData() {
        return orderService.clearAllData()
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<OrderResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.getById(id, JwtSupport.authenticatedUser(jwt))
                .flatMap(body -> Mono.just(ResponseEntity.ok(body)));
    }

    @GetMapping
    public Mono<ResponseEntity<List<OrderResponse>>> findAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE_SIZE) int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.findAll(userId, page, size, JwtSupport.authenticatedUser(jwt))
                .flatMap(result -> Mono.just(toPagedResponse(result)));
    }

    @PutMapping("/{id}/status")
    public Mono<ResponseEntity<OrderResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.updateStatus(id, request, JwtSupport.authenticatedUser(jwt))
                .flatMap(body -> Mono.just(ResponseEntity.ok(body)));
    }

    private ResponseEntity<List<OrderResponse>> toPagedResponse(Page<OrderResponse> pageResult) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(Pagination.TOTAL_COUNT_HEADER, String.valueOf(pageResult.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(pageResult.getContent());
    }
}
