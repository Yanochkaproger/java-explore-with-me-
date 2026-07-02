package ru.practicum.ewm.main.server.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.event.EventFullDto;
import ru.practicum.ewm.main.dto.event.EventShortDto;
import ru.practicum.ewm.main.dto.event.NewEventDto;
import ru.practicum.ewm.main.dto.event.UpdateEventAdminRequest;
import ru.practicum.ewm.main.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.main.server.entity.Category;
import ru.practicum.ewm.main.server.entity.Event;
import ru.practicum.ewm.main.server.entity.Location;
import ru.practicum.ewm.main.server.entity.User;
import ru.practicum.ewm.main.server.enums.EventAdminState;
import ru.practicum.ewm.main.server.enums.EventStatus;
import ru.practicum.ewm.main.server.enums.EventUserState;
import ru.practicum.ewm.main.server.exception.BadRequestException;
import ru.practicum.ewm.main.server.exception.ConflictException;
import ru.practicum.ewm.main.server.exception.NotFoundException;
import ru.practicum.ewm.main.server.mapper.EventMapper;
import ru.practicum.ewm.main.server.repository.CategoryRepository;
import ru.practicum.ewm.main.server.repository.EventRepository;
import ru.practicum.ewm.main.server.repository.RequestRepository;
import ru.practicum.ewm.main.server.repository.UserRepository;
import ru.practicum.ewm.main.server.specification.EventSpecification;
import ru.practicum.ewm.stats.client.StatsClient;
import ru.practicum.ewm.stats.dto.EndpointHit;
import ru.practicum.ewm.stats.dto.ViewStats;
import ru.practicum.ewm.main.server.exception.BadRequestException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;

    @Value("${server.application.name:ewm-service}")
    private String applicationName;

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        log.info("Создание события пользователем с id={}", userId);

        User initiator = checkUser(userId);
        checkDateAndTime(newEventDto.getEventDate());

        Category category = checkCategory(newEventDto.getCategory());

        Event event = eventMapper.toEntity(newEventDto, initiator, category);
        event.setCreatedOn(LocalDateTime.now());

        Event savedEvent = eventRepository.save(event);
        log.info("Событие с id={} успешно создано", savedEvent.getId());

        return EventMapper.toEventFullDto(savedEvent, 0L, 0L);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        log.info("Получение списка событий пользователя с id={}", userId);
        checkUser(userId);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "id"));
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        Map<Long, Long> viewsMap = getViewsForEvents(events);
        Map<Long, Long> confirmedMap = getConfirmedRequestsForEvents(events);

        return events.stream()
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        viewsMap.getOrDefault(event.getId(), 0L),
                        confirmedMap.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        log.info("Получение события с id={} пользователя с id={}", eventId, userId);
        checkUser(userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Событие с id=%d не найдено", eventId)));

        Map<Long, Long> viewsMap = getViewsForEvents(List.of(event));
        Map<Long, Long> confirmedMap = getConfirmedRequestsForEvents(List.of(event));

        return EventMapper.toEventFullDto(
                event,
                viewsMap.getOrDefault(eventId, 0L),
                confirmedMap.getOrDefault(eventId, 0L));
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("Обновление события с id={} пользователем с id={}", eventId, userId);
        checkUser(userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Событие с id=%d не найдено", eventId)));

        if (event.getState() == EventStatus.PUBLISHED) {
            throw new ConflictException("Событие можно обновлять только в статусах PENDING или CANCELED");
        }

        if (request.getEventDate() != null) {
            checkDateAndTime(request.getEventDate());
            event.setEventDate(request.getEventDate());
        }

        updateEventFields(event, request);

        if (request.getStateAction() != null) {
            EventUserState stateAction = EventUserState.valueOf(request.getStateAction());
            switch (stateAction) {
                case SEND_TO_REVIEW:
                    event.setState(EventStatus.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventStatus.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие с id={} успешно обновлено", eventId);

        Map<Long, Long> viewsMap = getViewsForEvents(List.of(updatedEvent));
        Map<Long, Long> confirmedMap = getConfirmedRequestsForEvents(List.of(updatedEvent));

        return EventMapper.toEventFullDto(
                updatedEvent,
                viewsMap.getOrDefault(eventId, 0L),
                confirmedMap.getOrDefault(eventId, 0L));
    }

    @Override
    public List<EventFullDto> getEventsForAdmin(List<Long> users, List<EventStatus> states, List<Long> categories,
                                                LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                int from, int size) {
        log.info("Получение списка событий для админа с фильтрацией");

        Pageable pageable = PageRequest.of(from / size, size);

        Specification<Event> spec = Specification.where(EventSpecification.hasUsers(users))
                .and(EventSpecification.hasStates(states))
                .and(EventSpecification.hasCategories(categories))
                .and(EventSpecification.hasRangeStart(rangeStart))
                .and(EventSpecification.hasRangeEnd(rangeEnd));

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        Map<Long, Long> viewsMap = getViewsForEvents(events);
        Map<Long, Long> confirmedMap = getConfirmedRequestsForEvents(events);

        return events.stream()
                .map(event -> EventMapper.toEventFullDto(
                        event,
                        viewsMap.getOrDefault(event.getId(), 0L),
                        confirmedMap.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        log.info("Обновление события с id={} администратором", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Событие с id=%d не найдено", eventId)));

        if (request.getAnnotation() != null && !request.getAnnotation().isBlank()) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            event.setDescription(request.getDescription());
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            event.setTitle(request.getTitle());
        }
        if (request.getCategory() != null) {
            Category category = checkCategory(request.getCategory());
            event.setCategory(category);
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getLocation() != null) {
            if (event.getLocation() == null) {
                Location location = new Location();
                event.setLocation(location);
            }
            event.getLocation().setLat(request.getLocation().getLat());
            event.getLocation().setLon(request.getLocation().getLon());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }

        if (request.getStateAction() != null) {
            EventAdminState action = EventAdminState.valueOf(request.getStateAction());

            switch (action) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventStatus.PENDING) {
                        throw new ConflictException("Событие можно публиковать только из статуса PENDING");
                    }
                    if (event.getEventDate().isBefore(LocalDateTime.now())) {
                        throw new ConflictException("Нельзя опубликовать событие, дата начала которого уже наступила");
                    }
                    event.setState(EventStatus.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;

                case REJECT_EVENT:
                    if (event.getState() == EventStatus.PUBLISHED) {
                        throw new ConflictException("Нельзя отклонить опубликованное событие");
                    }
                    event.setState(EventStatus.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие с id={} успешно обновлено администратором", eventId);

        Map<Long, Long> viewsMap = getViewsForEvents(List.of(updatedEvent));
        Map<Long, Long> confirmedMap = getConfirmedRequestsForEvents(List.of(updatedEvent));

        return EventMapper.toEventFullDto(
                updatedEvent,
                viewsMap.getOrDefault(eventId, 0L),
                confirmedMap.getOrDefault(eventId, 0L));
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort,
                                               int from, int size,
                                               HttpServletRequest request) { // ⚠️ Принимаем request
        log.info("Получение списка публичных событий");

        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new BadRequestException("Дата окончания не может быть раньше даты начала");
        }

        sendStats(request);

        int fetchSize = "VIEWS".equals(sort) ? from + size : size;
        Pageable pageable = createPageableWithSort(from, size, sort, fetchSize);

        Specification<Event> spec = Specification.where(EventSpecification.isPublished())
                .and(EventSpecification.hasText(text))
                .and(EventSpecification.hasCategories(categories))
                .and(EventSpecification.isPaid(paid))
                .and(EventSpecification.hasRangeStart(rangeStart))
                .and(EventSpecification.hasRangeEnd(rangeEnd));

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        Map<Long, Long> viewsMap = getViewsForEvents(events);
        Map<Long, Long> confirmedMap = getConfirmedRequestsForEvents(events);

        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = events.stream()
                    .filter(event -> event.getParticipantLimit() == 0 ||
                            event.getParticipantLimit() > confirmedMap.getOrDefault(event.getId(), 0L))
                    .collect(Collectors.toList());
        }

        if ("VIEWS".equals(sort)) {
            events = events.stream()
                    .sorted((e1, e2) -> Long.compare(
                            viewsMap.getOrDefault(e2.getId(), 0L),
                            viewsMap.getOrDefault(e1.getId(), 0L)))
                    .skip(from)
                    .limit(size)
                    .collect(Collectors.toList());
        }

        return events.stream()
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        viewsMap.getOrDefault(event.getId(), 0L),
                        confirmedMap.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto getPublicEvent(Long eventId, HttpServletRequest request) {
        log.info("Получение публичного события с id={}", eventId);

        Event event = eventRepository.findPublishedById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Событие с id=%d не найдено", eventId)));

        sendStats(request);

        Map<Long, Long> viewsMap = getViewsForEvents(List.of(event));
        Map<Long, Long> confirmedMap = getConfirmedRequestsForEvents(List.of(event));

        return EventMapper.toEventFullDto(
                event,
                viewsMap.getOrDefault(eventId, 0L),
                confirmedMap.getOrDefault(eventId, 0L));
    }


    private User checkUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Пользователь с id=%d не найден", userId)));
    }

    private Category checkCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Категория с id=%d не найдена", categoryId)));
    }

    private void checkDateAndTime(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException(
                    "Дата начала события должна быть не раньше, чем через 2 часа от текущего момента");
        }
    }

    private void updateEventFields(Event event, UpdateEventUserRequest request) {
        if (request.getAnnotation() != null && !request.getAnnotation().isBlank()) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            event.setDescription(request.getDescription());
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            event.setTitle(request.getTitle());
        }
        if (request.getCategory() != null) {
            Category category = checkCategory(request.getCategory());
            event.setCategory(category);
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getLocation() != null) {
            if (event.getLocation() == null) {
                Location location = new Location();
                event.setLocation(location);
            }
            event.getLocation().setLat(request.getLocation().getLat());
            event.getLocation().setLon(request.getLocation().getLon());
        }
    }

    private Pageable createPageableWithSort(int from, int size, String sort) {
        return createPageableWithSort(from, size, sort, size);
    }

    private Pageable createPageableWithSort(int from, int size, String sort, int fetchSize) {
        if ("EVENT_DATE".equals(sort)) {
            return PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "eventDate"));
        } else if ("VIEWS".equals(sort)) {
            return PageRequest.of(0, fetchSize);
        }
        return PageRequest.of(from / size, size);
    }

    private void sendStats(HttpServletRequest request) {
        try {
            EndpointHit hit = EndpointHit.builder()
                    .app(applicationName)
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build();

            statsClient.saveHit(hit);
            log.debug("Статистика успешно отправлена для URI: {}", request.getRequestURI());
        } catch (Exception e) {
            log.warn("Не удалось отправить статистику: {}", e.getMessage());
        }
    }

    private Map<Long, Long> getViewsForEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(10)); // Безопасный дефолт, если createdOn null

        LocalDateTime end = LocalDateTime.now();

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        try {
            List<ViewStats> stats = statsClient.getStats(start, end, uris, true);

            if (stats == null || stats.isEmpty()) {
                return Collections.emptyMap();
            }


            return stats.stream()
                    .filter(stat -> stat.getUri() != null && stat.getHits() != null)
                    .collect(Collectors.toMap(
                            stat -> {

                                String uri = stat.getUri();
                                return Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
                            },
                            ViewStats::getHits,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("Ошибка при получении статистики из stats-service: {}", e.getMessage());
            return Collections.emptyMap(); // Если сервис упал, возвращаем 0 просмотров, чтобы не ломать основную логику
        }
    }


    private Map<Long, Long> getConfirmedRequestsForEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
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
