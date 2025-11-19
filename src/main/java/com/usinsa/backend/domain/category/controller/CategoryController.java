package com.usinsa.backend.domain.category.controller;

import com.usinsa.backend.domain.category.dto.CategoryDto;
import com.usinsa.backend.domain.category.serivce.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    // 카테고리 생성
    @PostMapping
    public ResponseEntity<CategoryDto.Response> createCategory(@RequestBody CategoryDto.CreateReq req) {
        return ResponseEntity.ok(categoryService.create(req));
    }

    // 단일 카테고리 조회
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto.Response> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    // 전체 카테고리 조회
    @GetMapping
    public ResponseEntity<List<CategoryDto.Response>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    // 카테고리 이름 수정
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto.Response> updateCategory(
            @PathVariable Long id,
            @RequestParam String name
    ) {
        return ResponseEntity.ok(categoryService.update(id, name));
    }

    // 카테고리 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}