package ru.practicum.ewm.stats.client;

import ru.practicum.ewm.stats.dto.EndpointHit;
import ru.practicum.ewm.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsClient {
    void saveHit(EndpointHit hit);
    List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
