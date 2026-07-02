package ru.practicum.ewm.stats.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.dto.EndpointHit;
import ru.practicum.ewm.stats.dto.ViewStats;
import ru.practicum.ewm.stats.server.entity.EndpointHitEntity;
import ru.practicum.ewm.stats.server.repository.EndpointHitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final EndpointHitRepository repository;

    @Transactional
    public void saveHit(EndpointHit hit) {
        log.info("Saving hit: app={}, uri={}, ip={}, timestamp={}",
                hit.getApp(), hit.getUri(), hit.getIp(), hit.getTimestamp());

        EndpointHitEntity entity = EndpointHitEntity.builder()
                .app(hit.getApp())
                .uri(hit.getUri())
                .ip(hit.getIp())
                .timestamp(hit.getTimestamp())
                .build();

        repository.save(entity);
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        log.info("Getting stats: start={}, end={}, uris={}, unique={}", start, end, uris, unique);


        boolean hasUris = (uris != null && !uris.isEmpty());

        if (unique) {
            if (hasUris) {
                return repository.getStatsUniqueWithUris(start, end, uris);
            } else {
                return repository.getStatsUniqueWithoutUris(start, end);
            }
        } else {
            if (hasUris) {
                return repository.getStatsWithUris(start, end, uris);
            } else {
                return repository.getStatsWithoutUris(start, end);
            }
        }
    }
}


