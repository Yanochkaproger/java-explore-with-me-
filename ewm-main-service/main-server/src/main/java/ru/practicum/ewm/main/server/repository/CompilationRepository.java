package ru.practicum.ewm.main.server.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.main.server.entity.Compilation;

import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    Page<Compilation> findAll(Pageable pageable);

    @Query("SELECT c FROM Compilation c LEFT JOIN FETCH c.events WHERE c.id = :id")
    Optional<Compilation> findByIdWithEvents(@Param("id") Long id);

    @Query("SELECT c FROM Compilation c LEFT JOIN FETCH c.events")
    Page<Compilation> findAllWithEvents(Pageable pageable);

    @Query("SELECT c FROM Compilation c LEFT JOIN FETCH c.events WHERE c.pinned = :pinned")
    Page<Compilation> findAllByPinnedWithEvents(@Param("pinned") Boolean pinned, Pageable pageable);
}
