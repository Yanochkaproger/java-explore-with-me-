package ru.practicum.ewm.main.dto.compilation;

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
public class UpdateCompilationRequest {

    @Size(max = 50, message = "Заголовок подборки должен быть не более 50 символов")
    private String title;

    private Boolean pinned;

    private List<Long> events;
}
