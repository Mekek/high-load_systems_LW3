package ifmo.poster.order.service;

import ifmo.poster.contracts.enums.EventStatus;
import ifmo.poster.contracts.enums.OrderStatus;
import ifmo.poster.contracts.event.ReserveTicketsRequest;
import ifmo.poster.contracts.event.TicketReservationDto;
import ifmo.poster.contracts.exception.ConflictException;
import ifmo.poster.order.dto.CreateOrderItemRequest;
import ifmo.poster.order.dto.CreateOrderRequest;
import ifmo.poster.order.dto.OrderResponse;
import ifmo.poster.order.dto.UpdateOrderStatusRequest;
import ifmo.poster.security.AuthenticatedUser;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final UserClientService userClientService;
    private final EventClientService eventClientService;
    private final OrderPersistenceService persistenceService;

    public OrderService(
            UserClientService userClientService,
            EventClientService eventClientService,
            OrderPersistenceService persistenceService
    ) {
        this.userClientService = userClientService;
        this.eventClientService = eventClientService;
        this.persistenceService = persistenceService;
    }

    public Mono<OrderResponse> create(CreateOrderRequest request, AuthenticatedUser actor) {
        if (!actor.isSupervisor() && !actor.owns(request.userId())) {
            return Mono.error(new AccessDeniedException("Нельзя создать заказ для другого пользователя"));
        }
        return userClientService.getById(request.userId())
                .then(reserveAll(request.items()))
                .flatMap(reservations -> blocking(() -> persistenceService.createPendingOrder(request, reservations))
                        .onErrorResume(error -> compensate(reservations).then(Mono.error(error))));
    }

    public Mono<List<OrderResponse>> createBulk(
            List<CreateOrderRequest> requests,
            AuthenticatedUser actor
    ) {
        return Flux.fromIterable(requests)
                .concatMap(request -> create(request, actor))
                .collectList();
    }

    public Mono<Void> clearAllData() {
        return Mono.<Void>fromRunnable(persistenceService::clearAllData)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<OrderResponse> getById(Long id, AuthenticatedUser actor) {
        return blocking(() -> persistenceService.getById(id))
                .map(response -> requireOwnerOrSupervisor(response, actor));
    }

    public Mono<Page<OrderResponse>> findAll(
            Long userId,
            int page,
            int size,
            AuthenticatedUser actor
    ) {
        Long effectiveUserId = actor.isSupervisor() ? userId : actor.id();
        return blocking(() -> persistenceService.findAll(effectiveUserId, page, size));
    }

    public Mono<OrderResponse> updateStatus(
            Long id,
            UpdateOrderStatusRequest request,
            AuthenticatedUser actor
    ) {
        return getById(id, actor).flatMap(existing -> {
            if (request.status() == OrderStatus.CONFIRMED) {
                if (!actor.isSupervisor()) {
                    return Mono.error(new AccessDeniedException(
                            "Подтверждать заказы может только supervisor"
                    ));
                }
                return blocking(() -> persistenceService.confirm(id));
            }
            if (request.status() == OrderStatus.CANCELLED) {
                return cancel(id);
            }
            return Mono.error(new ConflictException(
                    "Поддерживаются только статусы CONFIRMED и CANCELLED"
            ));
        });
    }

    public Mono<OrderResponse> cancel(Long id) {
        return blocking(() -> persistenceService.reservationIds(id))
                .flatMap(reservationIds -> Flux.fromIterable(reservationIds)
                        .concatMap(reservationId -> eventClientService.cancelReservation(reservationId)
                                .onErrorResume(error -> {
                                    log.warn("Не удалось отменить резерв {}: {}", reservationId, error.toString());
                                    return Mono.empty();
                                })
                        )
                        .then(blocking(() -> persistenceService.markCancelled(id))));
    }

    private Mono<List<TicketReservationDto>> reserveAll(List<CreateOrderItemRequest> items) {
        List<TicketReservationDto> reserved = new ArrayList<>();
        return Flux.fromIterable(items)
                .concatMap(item -> validateAndReserve(item)
                        .doOnNext(reserved::add)
                        .onErrorResume(error -> compensate(reserved).then(Mono.error(error))))
                .collectList();
    }

    private Mono<TicketReservationDto> validateAndReserve(CreateOrderItemRequest item) {
        return eventClientService.getById(item.eventId())
                .flatMap(event -> {
                    if (event.status() != EventStatus.ACTIVE) {
                        return Mono.error(new ConflictException(
                                "Событие недоступно для заказа: id=" + item.eventId()
                        ));
                    }
                    return eventClientService.reserve(new ReserveTicketsRequest(
                            item.eventId(),
                            item.ticketTypeId(),
                            item.quantity()
                    ));
                });
    }

    private Mono<Void> compensate(List<TicketReservationDto> reservations) {
        return Flux.fromIterable(reservations)
                .concatMap(reservation -> eventClientService.cancelReservation(reservation.reservationId())
                        .onErrorResume(error -> {
                            log.warn(
                                    "Компенсация резерва {} не удалась: {}",
                                    reservation.reservationId(),
                                    error.toString()
                            );
                            return Mono.empty();
                        }))
                .then();
    }

    private <T> Mono<T> blocking(java.util.concurrent.Callable<T> action) {
        return Mono.fromCallable(action).subscribeOn(Schedulers.boundedElastic());
    }

    private OrderResponse requireOwnerOrSupervisor(OrderResponse response, AuthenticatedUser actor) {
        if (!actor.isSupervisor() && !actor.owns(response.userId())) {
            throw new AccessDeniedException("Нельзя просматривать чужой заказ");
        }
        return response;
    }
}
