package ifmo.poster.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ifmo.poster.contracts.enums.Role;
import ifmo.poster.contracts.enums.TicketStatus;
import ifmo.poster.order.entity.Order;
import ifmo.poster.order.entity.OrderItem;
import ifmo.poster.order.entity.Ticket;
import ifmo.poster.order.repository.TicketRepository;
import ifmo.poster.security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TicketAuthorizationTest {

    @Mock
    TicketRepository ticketRepository;

    TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository);
    }

    @Test
    void ordinaryUserCannotReadSomeoneElsesTicket() {
        when(ticketRepository.findDetailedById(5L)).thenReturn(Optional.of(ticketOwnedBy(2L)));

        StepVerifier.create(ticketService.getById(5L, user(1L)))
                .expectError(AccessDeniedException.class)
                .verify();
    }

    @Test
    void ordinaryUserListsOnlyOwnTickets() {
        when(ticketRepository.findByOrderUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticketOwnedBy(1L))));

        StepVerifier.create(ticketService.findAll(null, null, 0, 10, user(1L)))
                .expectNextMatches(page -> page.getTotalElements() == 1)
                .verifyComplete();

        verify(ticketRepository).findByOrderUserId(eq(1L), any(Pageable.class));
    }

    @Test
    void supervisorCanListAllTickets() {
        when(ticketRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticketOwnedBy(1L), ticketOwnedBy(2L))));

        StepVerifier.create(ticketService.findAll(
                        null,
                        null,
                        0,
                        10,
                        new AuthenticatedUser(99L, "boss", Role.SUPERVISOR)
                ))
                .expectNextMatches(page -> page.getTotalElements() == 2)
                .verifyComplete();

        verify(ticketRepository).findAll(any(Pageable.class));
    }

    private AuthenticatedUser user(Long id) {
        return new AuthenticatedUser(id, "user-" + id, Role.USER);
    }

    private Ticket ticketOwnedBy(Long userId) {
        Order order = new Order();
        order.setUserId(userId);
        OrderItem item = new OrderItem();
        item.setOrder(order);
        Ticket ticket = new Ticket();
        ticket.setId(5L);
        ticket.setEventId(10L);
        ticket.setTicketTypeId(1L);
        ticket.setStatus(TicketStatus.SOLD);
        ticket.setOrderItem(item);
        return ticket;
    }
}
