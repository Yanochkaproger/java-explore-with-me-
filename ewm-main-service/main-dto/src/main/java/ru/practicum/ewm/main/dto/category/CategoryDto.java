package ru.practicum.ewm.main.dto.category;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDto {

    private Long id;

    @Size(max = 50, message = "Название категории должно быть не более 50 символов")
    private String name;
}
