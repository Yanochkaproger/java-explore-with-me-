package ru.practicum.ewm.main.dto.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewCompilationDto {

    @NotBlank(message = "Заголовок подборки не может быть пустым")
    @Size(max = 50, message = "Заголовок подборки должен быть не более 50 символов")
    private String title;

    @Builder.Default
    private Boolean pinned = false;

    private List<Long> events;
}
