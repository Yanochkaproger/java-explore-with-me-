package ru.practicum.ewm.main.server.service;

import ru.practicum.ewm.main.dto.user.NewUserRequest;
import ru.practicum.ewm.main.dto.user.UserDto;

import java.util.List;

public interface UserService {
    UserDto createUser(NewUserRequest request);

    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    void deleteUser(Long userId);
}
