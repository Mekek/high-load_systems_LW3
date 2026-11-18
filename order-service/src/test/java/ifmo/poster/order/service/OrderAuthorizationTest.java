package ifmo.poster.order.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ifmo.poster.contracts.enums.OrderStatus;
import ifmo.poster.contracts.enums.Role;
import ifmo.poster.order.dto.CreateOrderItemRequest;
import ifmo.poster.order.dto.CreateOrderRequest;
import ifmo.poster.order.dto.OrderResponse;
import ifmo.poster.security.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OrderAuthorizationTest {

    @Mock
    UserClientService userClientService;

    @Mock
    EventClientService eventClientService;

    @Mock
    OrderPersistenceService persistenceService;

    OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(userClientService, eventClientService, persistenceService);
    }

    @Test
    void ordinaryUserCannotCreateOrderForSomeoneElse() {
        var request = new CreateOrderRequest(
                2L,
                List.of(new CreateOrderItemRequest(10L, 20L, 1))
        );

        StepVerifier.create(orderService.create(request, user(1L)))
                .expectError(AccessDeniedException.class)
                .verify();

        verifyNoInteractions(userClientService, eventClientService, persistenceService);
    }

    @Test
    void ordinaryUserListIsAlwaysLimitedToOwnOrders() {
        when(persistenceService.findAll(1L, 0, 20)).thenReturn(new PageImpl<>(List.of()));

        StepVerifier.create(orderService.findAll(999L, 0, 20, user(1L)))
                .expectNextCount(1)
                .verifyComplete();

        verify(persistenceService).findAll(1L, 0, 20);
    }

    @Test
    void ordinaryUserCannotReadSomeoneElsesOrder() {
        when(persistenceService.getById(10L)).thenReturn(new OrderResponse(
                10L,
                2L,
                OrderStatus.PENDING,
                LocalDateTime.now(),
                List.of()
        ));

        StepVerifier.create(orderService.getById(10L, user(1L)))
                .expectError(AccessDeniedException.class)
                .verify();
    }

    @Test
    void supervisorCanListAllOrders() {
        when(persistenceService.findAll(null, 0, 20)).thenReturn(new PageImpl<>(List.of()));
        AuthenticatedUser supervisor = new AuthenticatedUser(100L, "supervisor", Role.SUPERVISOR);

        StepVerifier.create(orderService.findAll(null, 0, 20, supervisor))
                .expectNextCount(1)
                .verifyComplete();

        verify(persistenceService).findAll(null, 0, 20);
    }

    private AuthenticatedUser user(Long id) {
        return new AuthenticatedUser(id, "user-" + id, Role.USER);
    }
}
