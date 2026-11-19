package ifmo.poster.order.repository;

import ifmo.poster.order.entity.Ticket;
import ifmo.poster.contracts.enums.TicketStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("""
            select t from Ticket t
            left join fetch t.orderItem oi
            left join fetch oi.order
            where t.id = :id
            """)
    Optional<Ticket> findDetailedById(@Param("id") Long id);

    long countByEventIdAndTicketTypeIdAndStatus(
            Long eventId,
            Long ticketTypeId,
            TicketStatus status
    );

    List<Ticket> findByEventIdAndTicketTypeIdAndStatus(
            Long eventId,
            Long ticketTypeId,
            TicketStatus status,
            Pageable pageable
    );

    List<Ticket> findByOrderItem_Order_Id(Long orderId);

    List<Ticket> findByOrderItem_Id(Long orderItemId);

    Page<Ticket> findByEventId(Long eventId, Pageable pageable);

    Page<Ticket> findByEventIdAndStatus(Long eventId, TicketStatus status, Pageable pageable);

    @Query("""
            select t from Ticket t
            join t.orderItem oi
            join oi.order o
            where o.userId = :userId
            """)
    Page<Ticket> findByOrderUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select t from Ticket t
            join t.orderItem oi
            join oi.order o
            where o.userId = :userId
              and t.eventId = :eventId
            """)
    Page<Ticket> findByOrderUserIdAndEventId(
            @Param("userId") Long userId,
            @Param("eventId") Long eventId,
            Pageable pageable
    );

    @Query("""
            select t from Ticket t
            join t.orderItem oi
            join oi.order o
            where o.userId = :userId
              and t.eventId = :eventId
              and t.status = :status
            """)
    Page<Ticket> findByOrderUserIdAndEventIdAndStatus(
            @Param("userId") Long userId,
            @Param("eventId") Long eventId,
            @Param("status") TicketStatus status,
            Pageable pageable
    );

    @Query("""
            select t from Ticket t
            join t.orderItem oi
            join oi.order o
            where o.userId = :userId
              and t.status = :status
            """)
    Page<Ticket> findByOrderUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") TicketStatus status,
            Pageable pageable
    );
    
    long countByTicketTypeId(Long ticketTypeId);
    
    /**
     * Блокирует выбранные строки (SELECT ... FOR UPDATE),
     * чтобы два заказа не забрали одни и те же AVAILABLE билеты.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from Ticket t
            where t.eventId = :eventId
              and t.ticketTypeId = :ticketTypeId
              and t.status = :status
            order by t.id
            """)
    List<Ticket> findAvailableForUpdate(
            @Param("eventId") Long eventId,
            @Param("ticketTypeId") Long ticketTypeId,
            @Param("status") TicketStatus status,
            Pageable pageable
    );
}