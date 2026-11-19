package ifmo.poster.order.service;

import ifmo.poster.contracts.enums.TicketStatus;
import ifmo.poster.contracts.exception.ResourceNotFoundException;
import ifmo.poster.contracts.support.Pagination;
import ifmo.poster.order.dto.TicketResponse;
import ifmo.poster.order.entity.Ticket;
import ifmo.poster.order.mapper.OrderMapper;
import ifmo.poster.order.repository.TicketRepository;
import ifmo.poster.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

    public Mono<TicketResponse> getById(Long id, AuthenticatedUser actor) {
        return Mono.fromCallable(() -> {
                    Ticket ticket = ticketRepository.findDetailedById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Билет не найден: id=" + id));
                    requireOwnerOrSupervisor(ticket, actor);
                    return OrderMapper.toTicketResponse(ticket);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Page<TicketResponse>> findAll(
            Long eventId,
            TicketStatus status,
            int page,
            int size,
            AuthenticatedUser actor
    ) {
        return Mono.fromCallable(() -> {
                    Pagination.validate(page, size);
                    PageRequest pageable = PageRequest.of(page, size);
                    Page<Ticket> tickets = actor.isSupervisor()
                            ? findForSupervisor(eventId, status, pageable)
                            : findForOwner(actor.id(), eventId, status, pageable);
                    return tickets.map(OrderMapper::toTicketResponse);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Page<Ticket> findForSupervisor(Long eventId, TicketStatus status, PageRequest pageable) {
        if (eventId != null && status != null) {
            return ticketRepository.findByEventIdAndStatus(eventId, status, pageable);
        }
        if (eventId != null) {
            return ticketRepository.findByEventId(eventId, pageable);
        }
        return ticketRepository.findAll(pageable);
    }

    private Page<Ticket> findForOwner(Long userId, Long eventId, TicketStatus status, PageRequest pageable) {
        if (eventId != null && status != null) {
            return ticketRepository.findByOrderUserIdAndEventIdAndStatus(userId, eventId, status, pageable);
        }
        if (eventId != null) {
            return ticketRepository.findByOrderUserIdAndEventId(userId, eventId, pageable);
        }
        if (status != null) {
            return ticketRepository.findByOrderUserIdAndStatus(userId, status, pageable);
        }
        return ticketRepository.findByOrderUserId(userId, pageable);
    }

    private void requireOwnerOrSupervisor(Ticket ticket, AuthenticatedUser actor) {
        if (actor.isSupervisor()) {
            return;
        }
        Long ownerId = ticket.getOrderItem() == null || ticket.getOrderItem().getOrder() == null
                ? null
                : ticket.getOrderItem().getOrder().getUserId();
        if (!actor.owns(ownerId)) {
            throw new AccessDeniedException("Нет доступа к чужому билету");
        }
    }
}
