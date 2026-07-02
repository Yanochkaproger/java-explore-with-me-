package ru.practicum.ewm.main.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.main.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.main.server.entity.Event;
import ru.practicum.ewm.main.server.entity.Request;
import ru.practicum.ewm.main.server.entity.User;
import ru.practicum.ewm.main.server.enums.EventStatus;
import ru.practicum.ewm.main.server.enums.RequestStatus;
import ru.practicum.ewm.main.server.exception.ConflictException;
import ru.practicum.ewm.main.server.exception.NotFoundException;
import ru.practicum.ewm.main.server.mapper.RequestMapper;
import ru.practicum.ewm.main.server.repository.EventRepository;
import ru.practicum.ewm.main.server.repository.RequestRepository;
import ru.practicum.ewm.main.server.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;


    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Создание заявки на участие: userId={}, eventId={}", userId, eventId);

        User requester = checkUser(userId);
        Event event = checkEvent(eventId);

        validateNewRequest(event, userId, eventId);

        Request request = Request.builder()
                .requester(requester)
                .event(event)
                .created(LocalDateTime.now())
                .status(determineRequestStatus(event, eventId))
                .build();

        Request savedRequest = requestRepository.save(request);
        log.info("Заявка с id={} создана со статусом {}", savedRequest.getId(), savedRequest.getStatus());

        return RequestMapper.toDto(savedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение списка заявок пользователя с id={}", userId);
        checkUser(userId);

        List<Request> requests = requestRepository.findAllByRequesterId(userId);
        return requests.stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Получение списка заявок для события с id={}", eventId);
        checkUser(userId);
        checkEventByInitiator(userId, eventId);

        List<Request> requests = requestRepository.findAllByEventId(eventId);
        return requests.stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestsStatus(Long userId, Long eventId,
                                                               EventRequestStatusUpdateRequest request) {
        log.info("Массовое обновление заявок: userId={}, eventId={}, requestIds={}, status={}",
                userId, eventId, request.getRequestIds(), request.getStatus());

        checkUser(userId);
        Event event = checkEventByInitiator(userId, eventId);

        RequestStatus newStatus = RequestStatus.valueOf(request.getStatus());

        List<Request> confirmedRequests = new ArrayList<>();
        List<Request> rejectedRequests = new ArrayList<>();

        List<Request> requestsToUpdate = requestRepository.findAllByIdIn(request.getRequestIds());

        for (Request req : requestsToUpdate) {
            if (!req.getEvent().getId().equals(eventId)) {
                throw new NotFoundException("Заявка с id=" + req.getId() + " не найдена");
            }
        }

        if (newStatus == RequestStatus.CONFIRMED) {
            Long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            int freeSlots = event.getParticipantLimit() - confirmedCount.intValue();

            if (event.getParticipantLimit() > 0 && freeSlots <= 0) {
                throw new ConflictException("Достигнут лимит подтверждённых заявок на участие в событии");
            }

            for (Request req : requestsToUpdate) {
                if (freeSlots > 0) {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmedRequests.add(req);
                    freeSlots--;
                } else {
                    req.setStatus(RequestStatus.REJECTED);
                    rejectedRequests.add(req);
                }
            }
        } else if (newStatus == RequestStatus.REJECTED) {
            for (Request req : requestsToUpdate) {
                req.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(req);
            }
        } else {
            throw new ConflictException("Недопустимый статус: " + newStatus);
        }

        requestRepository.saveAll(requestsToUpdate);

        log.info("Обновлено заявок: подтверждено={}, отклонено={}",
                confirmedRequests.size(), rejectedRequests.size());

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests.stream()
                        .map(RequestMapper::toDto)
                        .collect(Collectors.toList()))
                .rejectedRequests(rejectedRequests.stream()
                        .map(RequestMapper::toDto)
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена заявки: userId={}, requestId={}", userId, requestId);
        checkUser(userId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка с id=" + requestId + " не найдена"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Заявка с id=" + requestId + " не найдена");
        }

        if (request.getStatus() == RequestStatus.CANCELED || request.getStatus() == RequestStatus.REJECTED) {
            throw new ConflictException("Заявка уже отменена или отклонена");
        }

        request.setStatus(RequestStatus.CANCELED);
        Request updatedRequest = requestRepository.save(request);

        log.info("Заявка с id={} отменена", requestId);
        return RequestMapper.toDto(updatedRequest);
    }

    private User checkUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Event checkEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    private Event checkEventByInitiator(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Событие с id=" + eventId + " не найдено или не принадлежит пользователю"));
    }

    private void validateNewRequest(Event event, Long userId, Long eventId) {
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Нельзя создать заявку на своё собственное событие");
        }
        if (event.getState() != EventStatus.PUBLISHED) {
            throw new ConflictException("Нельзя создать заявку на неопубликованное событие");
        }
        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Заявка на это событие уже существует");
        }
    }

    private RequestStatus determineRequestStatus(Event event, Long eventId) {
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            return RequestStatus.CONFIRMED;
        }

        Long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит подтверждённых заявок на участие в событии");
        }

        return RequestStatus.PENDING;
    }
}
