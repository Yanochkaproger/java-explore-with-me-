package ru.practicum.ewm.main.server.exception;

public class ConditionNotMetException extends RuntimeException {
    public ConditionNotMetException(String message) {
        super(message);
    }
}
