package com.usinsa.backend.domain.category.serivce;

import com.usinsa.backend.domain.category.dto.CategoryDto;
import com.usinsa.backend.domain.category.entity.Category;
import com.usinsa.backend.domain.category.repository.CategoryRepository;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // 카테고리 생성
    @Transactional
    public CategoryDto.Response create(CategoryDto.CreateReq req) {
        Category parent = null;

        if (req.getParentId() != null) {
            parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        Category category = Category.builder()
                .name(req.getName())
                .parent(parent)
                .build();

        if (parent != null) {
            parent.addChild(category);
        }

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    // 단일 카테고리 조회
    @Transactional(readOnly = true)
    public CategoryDto.Response findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        return toResponse(category);
    }

    // 전체 카테고리 조회
    @Transactional(readOnly = true)
    public List<CategoryDto.Response> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 카테고리 이름 수정
    @Transactional
    public CategoryDto.Response update(Long id, String newName) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        category.setName(newName);

        Category updated = categoryRepository.save(category);
        return toResponse(updated);
    }

    // 카테고리 삭제
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        categoryRepository.delete(category);
    }

    // Service 내에서 DTO 변환
    private CategoryDto.Response toResponse(Category category) {
        List<String> productNames = category.getProducts().stream()
                .map(Product::getName)
                .collect(Collectors.toList());

        return CategoryDto.Response.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .productNames(productNames)
                .build();
    }
}