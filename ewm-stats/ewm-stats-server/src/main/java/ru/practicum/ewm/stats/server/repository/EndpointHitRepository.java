package ru.practicum.ewm.stats.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.stats.dto.ViewStats;
import ru.practicum.ewm.stats.server.entity.EndpointHitEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface EndpointHitRepository extends JpaRepository<EndpointHitEntity, Long> {


    @Query("SELECT new ru.practicum.ewm.stats.dto.ViewStats(e.app, e.uri, COUNT(e.id)) " +
            "FROM EndpointHitEntity e " +
            "WHERE e.timestamp BETWEEN :start AND :end " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(e.id) DESC")
    List<ViewStats> getStatsWithoutUris(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);


    @Query("SELECT new ru.practicum.ewm.stats.dto.ViewStats(e.app, e.uri, COUNT(e.id)) " +
            "FROM EndpointHitEntity e " +
            "WHERE e.timestamp BETWEEN :start AND :end " +
            "AND e.uri IN :uris " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(e.id) DESC")
    List<ViewStats> getStatsWithUris(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end,
                                     @Param("uris") List<String> uris);


    @Query("SELECT new ru.practicum.ewm.stats.dto.ViewStats(e.app, e.uri, COUNT(DISTINCT e.ip)) " +
            "FROM EndpointHitEntity e " +
            "WHERE e.timestamp BETWEEN :start AND :end " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(DISTINCT e.ip) DESC")
    List<ViewStats> getStatsUniqueWithoutUris(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);


    @Query("SELECT new ru.practicum.ewm.stats.dto.ViewStats(e.app, e.uri, COUNT(DISTINCT e.ip)) " +
            "FROM EndpointHitEntity e " +
            "WHERE e.timestamp BETWEEN :start AND :end " +
            "AND e.uri IN :uris " +
            "GROUP BY e.app, e.uri " +
            "ORDER BY COUNT(DISTINCT e.ip) DESC")
    List<ViewStats> getStatsUniqueWithUris(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end,
                                           @Param("uris") List<String> uris);
}
