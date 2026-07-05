package ru.practicum.ewm.main.server.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.main.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.main.server.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
public class PrivateRequestController {

    private final RequestService requestService;



    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable Long userId,
                                                 @RequestParam Long eventId) {
        log.info("POST /users/{}/requests?eventId={}: создание заявки на участие", userId, eventId);
        return requestService.createRequest(userId, eventId);
    }



    @GetMapping("/requests")
    public List<ParticipationRequestDto> getUserRequests(@PathVariable Long userId) {
        log.info("GET /users/{}/requests: получение списка заявок пользователя", userId);
        return requestService.getUserRequests(userId);
    }



    @GetMapping("/requests/{requestId}")
    public ParticipationRequestDto getUserRequest(@PathVariable Long userId,
                                                  @PathVariable Long requestId) {
        log.info("GET /users/{}/requests/{}: получение конкретной заявки", userId, requestId);
        List<ParticipationRequestDto> requests = requestService.getUserRequests(userId);

        return requests.stream()
                .filter(req -> req.getId().equals(requestId))
                .findFirst()
                .orElseThrow(() -> new ru.practicum.ewm.main.server.exception.NotFoundException(
                        "Заявка с id=" + requestId + " не найдена"));
    }



    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId,
                                                 @PathVariable Long requestId) {
        log.info("PATCH /users/{}/requests/{}/cancel: отмена заявки", userId, requestId);
        return requestService.cancelRequest(userId, requestId);
    }

    // Получение списка заявок для события (для организатора)

    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId,
                                                          @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}/requests: получение списка заявок для события", userId, eventId);
        return requestService.getEventRequests(userId, eventId);
    }

    // Массовое подтверждение/отклонение заявок организатором

    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestsStatus(@PathVariable Long userId,
                                                               @PathVariable Long eventId,
                                                               @Valid @RequestBody EventRequestStatusUpdateRequest request) {
        log.info("PATCH /users/{}/events/{}/requests: массовое обновление заявок", userId, eventId);
        return requestService.updateRequestsStatus(userId, eventId, request);
    }
}
