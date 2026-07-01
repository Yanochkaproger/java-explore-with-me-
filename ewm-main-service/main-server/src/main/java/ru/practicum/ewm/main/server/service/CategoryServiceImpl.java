package ru.practicum.ewm.main.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.category.CategoryDto;
import ru.practicum.ewm.main.dto.category.NewCategoryDto;
import ru.practicum.ewm.main.server.entity.Category;
import ru.practicum.ewm.main.server.exception.ConflictException;
import ru.practicum.ewm.main.server.exception.NotFoundException;
import ru.practicum.ewm.main.server.mapper.CategoryMapper;
import ru.practicum.ewm.main.server.repository.CategoryRepository;
import ru.practicum.ewm.main.server.repository.EventRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Создание категории: {}", newCategoryDto.getName());

        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Категория с именем '" + newCategoryDto.getName() + "' уже существует");
        }

        Category category = CategoryMapper.toCategory(newCategoryDto);
        Category savedCategory = categoryRepository.save(category);

        log.info("Категория с id={} успешно создана", savedCategory.getId());
        return CategoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Удаление категории с id={}", catId);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));

        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Невозможно удалить категорию, так как существуют события, связанные с ней");
        }

        categoryRepository.delete(category);
        log.info("Категория с id={} успешно удалена", catId);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        log.info("Обновление категории с id={}", catId);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));

        if (!category.getName().equals(categoryDto.getName()) &&
                categoryRepository.existsByName(categoryDto.getName())) {
            throw new ConflictException("Категория с именем '" + categoryDto.getName() + "' уже существует");
        }

        category.setName(categoryDto.getName());
        Category updatedCategory = categoryRepository.save(category);

        log.info("Категория с id={} успешно обновлена", catId);
        return CategoryMapper.toCategoryDto(updatedCategory);
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        log.info("Получение категории с id={}", catId);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));

        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        log.info("Получение списка категорий: from={}, size={}", from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Category> categoriesPage = categoryRepository.findAllCategories(pageable);

        return categoriesPage.getContent().stream()
                .map(CategoryMapper::toCategoryDto)
                .collect(Collectors.toList());
    }
}
