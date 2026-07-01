package ru.practicum.ewm.main.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.main.server.entity.Event;
import ru.practicum.ewm.main.server.enums.EventStatus;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    List<Event> findAllByInitiatorId(Long initiatorId, org.springframework.data.domain.Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);

    @Query("SELECT e FROM Event e WHERE e.state = 'PUBLISHED' AND e.id = :id")
    Optional<Event> findPublishedById(@Param("id") Long id);

    boolean existsByCategoryId(Long categoryId);
}

