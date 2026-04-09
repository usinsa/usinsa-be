package com.usinsa.backend.domain.category.service;

import com.usinsa.backend.domain.category.dto.CategoryDto;
import com.usinsa.backend.domain.category.entity.Category;
import com.usinsa.backend.domain.category.repository.CategoryRepository;
import com.usinsa.backend.domain.category.serivce.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    private Category testCategory;
    private Category parentCategory;

    @BeforeEach
    void setUp() {
        parentCategory = Category.builder()
                .id(1L)
                .name("의류")
                .parent(null)
                .children(new ArrayList<>())
                .products(new ArrayList<>())
                .build();

        testCategory = Category.builder()
                .id(2L)
                .name("상의")
                .parent(parentCategory)
                .children(new ArrayList<>())
                .products(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("카테고리 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("최상위 카테고리를 생성한다")
        void create_RootCategory_Success() {
            // given
            CategoryDto.CreateReq createReq = CategoryDto.CreateReq.builder()
                    .name("신발")
                    .parentId(null)
                    .build();

            Category newCategory = Category.builder()
                    .id(3L)
                    .name("신발")
                    .parent(null)
                    .children(new ArrayList<>())
                    .products(new ArrayList<>())
                    .build();

            given(categoryRepository.save(any(Category.class))).willReturn(newCategory);

            // when
            CategoryDto.Response result = categoryService.create(createReq);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("신발");
            assertThat(result.getParentId()).isNull();
        }

        @Test
        @DisplayName("하위 카테고리를 생성한다")
        void create_ChildCategory_Success() {
            // given
            CategoryDto.CreateReq createReq = CategoryDto.CreateReq.builder()
                    .name("하의")
                    .parentId(1L)
                    .build();

            Category childCategory = Category.builder()
                    .id(4L)
                    .name("하의")
                    .parent(parentCategory)
                    .children(new ArrayList<>())
                    .products(new ArrayList<>())
                    .build();

            given(categoryRepository.findById(1L)).willReturn(Optional.of(parentCategory));
            given(categoryRepository.save(any(Category.class))).willReturn(childCategory);

            // when
            CategoryDto.Response result = categoryService.create(createReq);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("하의");
            assertThat(result.getParentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("부모 카테고리가 없으면 예외가 발생한다")
        void create_ParentNotFound_Fail() {
            // given
            CategoryDto.CreateReq createReq = CategoryDto.CreateReq.builder()
                    .name("하의")
                    .parentId(999L)
                    .build();

            given(categoryRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> categoryService.create(createReq))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상위 카테고리를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("카테고리 조회 테스트")
    class FindTest {

        @Test
        @DisplayName("ID로 카테고리를 조회한다")
        void findById_Success() {
            // given
            given(categoryRepository.findById(anyLong())).willReturn(Optional.of(testCategory));

            // when
            CategoryDto.Response result = categoryService.findById(2L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getName()).isEqualTo("상의");
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 조회 시 예외가 발생한다")
        void findById_NotFound_Fail() {
            // given
            given(categoryRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> categoryService.findById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("카테고리를 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("모든 카테고리를 조회한다")
        void findAll_Success() {
            // given
            given(categoryRepository.findAll()).willReturn(Arrays.asList(parentCategory, testCategory));

            // when
            List<CategoryDto.Response> results = categoryService.findAll();

            // then
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("카테고리 수정 테스트")
    class UpdateTest {

        @Test
        @DisplayName("정상적으로 카테고리 이름을 수정한다")
        void update_Success() {
            // given
            given(categoryRepository.findById(anyLong())).willReturn(Optional.of(testCategory));
            given(categoryRepository.save(any(Category.class))).willReturn(testCategory);

            // when
            CategoryDto.Response result = categoryService.update(2L, "아우터");

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 수정 시 예외가 발생한다")
        void update_NotFound_Fail() {
            // given
            given(categoryRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> categoryService.update(999L, "새이름"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("카테고리를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("카테고리 삭제 테스트")
    class DeleteTest {

        @Test
        @DisplayName("정상적으로 카테고리를 삭제한다")
        void delete_Success() {
            // given
            given(categoryRepository.findById(anyLong())).willReturn(Optional.of(testCategory));
            doNothing().when(categoryRepository).delete(any(Category.class));

            // when
            categoryService.delete(2L);

            // then
            verify(categoryRepository).delete(testCategory);
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 삭제 시 예외가 발생한다")
        void delete_NotFound_Fail() {
            // given
            given(categoryRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> categoryService.delete(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("카테고리를 찾을 수 없습니다.");
        }
    }
}
