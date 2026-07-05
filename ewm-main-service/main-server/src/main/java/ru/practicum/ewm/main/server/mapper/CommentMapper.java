package ru.practicum.ewm.main.server.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.main.dto.comment.CommentDto;
import ru.practicum.ewm.main.server.entity.Comment;

@UtilityClass
public class CommentMapper {
    public CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .eventId(comment.getEvent().getId())
                .authorName(comment.getAuthor().getName())
                .created(comment.getCreated())
                .build();
    }
}

