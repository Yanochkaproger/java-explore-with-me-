package ru.practicum.ewm.stats.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.stats.dto.EndpointHit;
import ru.practicum.ewm.stats.dto.ViewStats;
import ru.practicum.ewm.stats.server.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;

@RestController
@Slf4j
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@Valid @RequestBody EndpointHit hit) {
        log.info("POST /hit: app={}, uri={}, ip={}, timestamp={}",
                hit.getApp(), hit.getUri(), hit.getIp(), hit.getTimestamp());
        statsService.saveHit(hit);
    }

    @GetMapping("/stats")
    public List<ViewStats> getStats(
            @RequestParam @DateTimeFormat(pattern = DATE_FORMAT) LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = DATE_FORMAT) LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique) {
        log.info("GET /stats: start={}, end={}, uris={}, unique={}", start, end, uris, unique);


        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала диапазона не может быть позже даты окончания");
        }


        return statsService.getStats(start, end, uris, unique);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgumentException(final IllegalArgumentException e) {
        log.error("Ошибка валидации параметров: {}", e.getMessage());
        return Map.of(
                "status", "BAD_REQUEST",
                "reason", "Incorrectly made request.",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        );
    }
}

