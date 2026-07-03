package ru.practicum.ewm.main.server.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.main.dto.category.CategoryDto;
import ru.practicum.ewm.main.dto.category.NewCategoryDto;
import ru.practicum.ewm.main.server.entity.Category;

@UtilityClass
public class CategoryMapper {

    public Category toCategory(NewCategoryDto dto) {
        return Category.builder()
                .name(dto.getName())
                .build();
    }

    public CategoryDto toCategoryDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
