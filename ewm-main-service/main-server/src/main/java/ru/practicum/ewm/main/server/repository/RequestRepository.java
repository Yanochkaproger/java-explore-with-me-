package ru.practicum.ewm.main.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.main.server.entity.Request;
import ru.practicum.ewm.main.server.enums.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findAllByRequesterId(Long requesterId);

    List<Request> findAllByEventId(Long eventId);

    List<Request> findAllByEventIdAndStatus(Long eventId, RequestStatus status);

    Optional<Request> findByEventIdAndRequesterId(Long eventId, Long requesterId);

    @Query("SELECT COUNT(r) FROM Request r WHERE r.event.id = :eventId AND r.status = :status")
    Long countByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") RequestStatus status);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<Request> findAllByIdIn(List<Long> ids);

    @Query("SELECT r.event.id, COUNT(r) FROM Request r " +
            "WHERE r.event.id IN :eventIds AND r.status = 'CONFIRMED' " +
            "GROUP BY r.event.id")
    List<Object[]> countConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);
}
