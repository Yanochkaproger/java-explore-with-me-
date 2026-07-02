package ru.practicum.ewm.main.server.mapper;

import ru.practicum.ewm.main.dto.compilation.CompilationDto;
import ru.practicum.ewm.main.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.main.server.entity.Compilation;
import ru.practicum.ewm.main.server.entity.Event;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CompilationMapper {

    public static Compilation toEntity(NewCompilationDto dto) {
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned() != null ? dto.getPinned() : false)
                .build();
    }

    public static CompilationDto toDto(Compilation compilation, Map<Long, Long> viewsMap,
                                       Map<Long, Long> confirmedMap) {
        Set<Event> events = compilation.getEvents();
        if (events == null) {
            events = Collections.emptySet();
        }

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(events.stream()
                        .map(event -> EventMapper.toEventShortDto(
                                event,
                                viewsMap.getOrDefault(event.getId(), 0L),
                                confirmedMap.getOrDefault(event.getId(), 0L)))
                        .collect(Collectors.toList()))
                .build();
    }
}
