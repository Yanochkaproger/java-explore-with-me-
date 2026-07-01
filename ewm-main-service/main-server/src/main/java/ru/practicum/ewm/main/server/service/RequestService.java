package ru.practicum.ewm.main.server.service;

import ru.practicum.ewm.main.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.main.dto.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    /**
     * Создание заявки на участие в событии.
     */
    ParticipationRequestDto createRequest(Long userId, Long eventId);

    /**
     * Получение списка заявок текущего пользователя.
     */
    List<ParticipationRequestDto> getUserRequests(Long userId);

    /**
     * Получение списка заявок для организатора события.
     */
    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    /**
     * Массовое подтверждение/отклонение заявок организатором.
     */
    EventRequestStatusUpdateResult updateRequestsStatus(Long userId, Long eventId,
                                                        EventRequestStatusUpdateRequest request);

    /**
     * Отмена заявки пользователем.
     */
    ParticipationRequestDto cancelRequest(Long userId, Long requestId);
}