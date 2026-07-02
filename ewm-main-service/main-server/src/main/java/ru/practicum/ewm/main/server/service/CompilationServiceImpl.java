package ru.practicum.ewm.main.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.compilation.CompilationDto;
import ru.practicum.ewm.main.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.main.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.main.server.entity.Compilation;
import ru.practicum.ewm.main.server.entity.Event;
import ru.practicum.ewm.main.server.exception.NotFoundException;
import ru.practicum.ewm.main.server.mapper.CompilationMapper;
import ru.practicum.ewm.main.server.repository.CompilationRepository;
import ru.practicum.ewm.main.server.repository.EventRepository;
import ru.practicum.ewm.main.server.repository.RequestRepository;
import ru.practicum.ewm.stats.client.StatsClient;
import ru.practicum.ewm.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto dto) {
        log.info("Создание подборки: title={}", dto.getTitle());

        Compilation compilation = CompilationMapper.toEntity(dto);

        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            List<Event> events = eventRepository.findAllById(dto.getEvents());
            compilation.setEvents(events.stream().collect(Collectors.toSet()));
        }

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Подборка с id={} успешно создана", savedCompilation.getId());

        return toDtoWithStats(savedCompilation);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest dto) {
        log.info("Обновление подборки с id={}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            compilation.setTitle(dto.getTitle());
        }

        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }

        if (dto.getEvents() != null) {
            if (dto.getEvents().isEmpty()) {
                compilation.setEvents(Collections.emptySet());
            } else {
                List<Event> events = eventRepository.findAllById(dto.getEvents());
                compilation.setEvents(events.stream().collect(Collectors.toSet()));
            }
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Подборка с id={} успешно обновлена", compId);

        return toDtoWithStats(updatedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки с id={}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id=" + compId + " не найдена");
        }

        compilationRepository.deleteById(compId);
        log.info("Подборка с id={} успешно удалена", compId);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("Получение списка подборок: pinned={}, from={}, size={}", pinned, from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Compilation> compilations;

        if (pinned != null) {
            compilations = compilationRepository.findAllByPinnedWithEvents(pinned, pageable);
        } else {
            compilations = compilationRepository.findAllWithEvents(pageable);
        }

        return compilations.getContent().stream()
                .map(this::toDtoWithStats)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        log.info("Получение подборки с id={}", compId);

        Compilation compilation = compilationRepository.findByIdWithEvents(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        return toDtoWithStats(compilation);
    }

    private CompilationDto toDtoWithStats(Compilation compilation) {
        List<Event> events = compilation.getEvents() != null
                ? new ArrayList<>(compilation.getEvents())
                : Collections.emptyList();

        Map<Long, Long> viewsMap = getViewsForEvents(events);
        Map<Long, Long> confirmedMap = getConfirmedRequestsForEvents(events);

        return CompilationMapper.toDto(compilation, viewsMap, confirmedMap);
    }

    private Map<Long, Long> getViewsForEvents(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<String> uris = events.stream()
                    .map(event -> "/events/" + event.getId())
                    .collect(Collectors.toList());

            LocalDateTime start = events.stream()
                    .map(Event::getCreatedOn)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now().minusYears(1));

            List<ViewStats> stats = statsClient.getStats(start, LocalDateTime.now(), uris, true);

            return stats.stream()
                    .collect(Collectors.toMap(
                            stat -> Long.parseLong(stat.getUri().replace("/events/", "")),
                            ViewStats::getHits,
                            (a, b) -> a
                    ));
        } catch (Exception e) {
            log.warn("Не удалось получить статистику просмотров: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<Long, Long> getConfirmedRequestsForEvents(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        List<Object[]> counts = requestRepository.countConfirmedByEventIds(eventIds);

        return counts.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
