package dev.tphucnha.moneylogger.service.impl;

import dev.tphucnha.moneylogger.domain.Category;
import dev.tphucnha.moneylogger.repository.CategoryRepository;
import dev.tphucnha.moneylogger.security.SecurityUtils;
import dev.tphucnha.moneylogger.service.CategoryService;
import dev.tphucnha.moneylogger.service.dto.CategoryDTO;
import dev.tphucnha.moneylogger.service.mapper.CategoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;

/**
 * Service Implementation for managing {@link Category}.
 */
@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public CategoryDTO save(CategoryDTO categoryDTO) {
        log.debug("Request to save Category : {}", categoryDTO);
        Category category = categoryMapper.toEntity(categoryDTO);
        if (category.getId() != null) {
            Category target = categoryRepository.findById(categoryDTO.getId()).orElseThrow(EntityNotFoundException::new);
            if (!target.getCreatedBy().equals(SecurityUtils.getCurrentUserLogin().orElse("")))
                throw new AccessDeniedException("Access denied");
            else
                return categoryMapper.toDto(categoryRepository.save(category));
        }
        category = categoryRepository.save(category);
        return categoryMapper.toDto(category);
    }

    @Override
    public Optional<CategoryDTO> partialUpdate(CategoryDTO categoryDTO) {
        log.debug("Request to partially update Category : {}", categoryDTO);
        Optional<Category> target = categoryRepository.findById(categoryDTO.getId());
        if (!target.orElseThrow(EntityNotFoundException::new).getCreatedBy()
            .equals(SecurityUtils.getCurrentUserLogin().orElse("")))
            throw new AccessDeniedException("Access denied");

        return target.map(
            existingCategory -> {
                categoryMapper.partialUpdate(existingCategory, categoryDTO);
                return existingCategory;
            }
        )
            .map(categoryRepository::save)
            .map(categoryMapper::toDto);
    }

//    @Override
//    @Transactional(readOnly = true)
//    public Page<CategoryDTO> findAll(Pageable pageable) {
//        log.debug("Request to get all Categories");
//        return categoryRepository.findAll(pageable).map(categoryMapper::toDto);
//    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoryDTO> findOne(Long id) {
        log.debug("Request to get Category : {}", id);
        Optional<Category> target = categoryRepository.findById(id);
        if (target.isPresent() && !target.get().getCreatedBy()
            .equals(SecurityUtils.getCurrentUserLogin().orElse("")))
            throw new AccessDeniedException("Access denied");

        return target.map(categoryMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete Category : {}", id);
        Category target = categoryRepository.findById(id).orElseThrow();
        if (!target.getCreatedBy().equals(SecurityUtils.getCurrentUserLogin().orElse("")))
            throw new AccessDeniedException("Access denied");

        categoryRepository.delete(target);
    }
}
