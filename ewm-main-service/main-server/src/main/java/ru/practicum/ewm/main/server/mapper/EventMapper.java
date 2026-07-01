package ru.practicum.ewm.main.server.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.main.dto.event.EventFullDto;
import ru.practicum.ewm.main.dto.event.EventShortDto;
import ru.practicum.ewm.main.dto.event.LocationDto;
import ru.practicum.ewm.main.dto.event.NewEventDto;
import ru.practicum.ewm.main.server.entity.Category;
import ru.practicum.ewm.main.server.entity.Event;
import ru.practicum.ewm.main.server.entity.Location;
import ru.practicum.ewm.main.server.entity.User;
import ru.practicum.ewm.main.server.enums.EventStatus;

@Component
public class EventMapper {

    public Event toEntity(NewEventDto dto, User initiator, Category category) {
        return Event.builder()
                .annotation(dto.getAnnotation())
                .category(category)
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .initiator(initiator)
                .location(toLocation(dto.getLocation()))
                .paid(dto.getPaid())
                .participantLimit(dto.getParticipantLimit())
                .requestModeration(dto.getRequestModeration())
                .state(EventStatus.PENDING)
                .title(dto.getTitle())
                .build();
    }

    public static Location toLocation(LocationDto dto) {
        if (dto == null) return null;
        return Location.builder()
                .lat(dto.getLat())
                .lon(dto.getLon())
                .build();
    }

    public static LocationDto toLocationDto(Location location) {
        if (location == null) return null;
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }

    public EventShortDto toEventShortDto(Event event) {
        return toEventShortDto(event, 0L, 0L);
    }

    public static EventShortDto toEventShortDto(Event event, Long views, Long confirmedRequests) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(confirmedRequests != null ? confirmedRequests : 0L)
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views != null ? views : 0L)
                .build();
    }

    public EventFullDto toEventFullDto(Event event) {
        return toEventFullDto(event, 0L, 0L);
    }

    public static EventFullDto toEventFullDto(Event event, Long views, Long confirmedRequests) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(confirmedRequests != null ? confirmedRequests : 0L)
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                .location(toLocationDto(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState().name())
                .title(event.getTitle())
                .views(views != null ? views : 0L)
                .build();
    }
}
