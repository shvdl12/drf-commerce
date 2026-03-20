package com.drf.product.controller;

import com.drf.common.exception.BusinessException;
import com.drf.product.common.exception.ErrorCode;
import com.drf.product.model.request.CategoryCreateRequest;
import com.drf.product.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(value = AdminCategoryController.class)
public class AdminCategoryControllerTest extends BaseControllerTest {

    @MockitoBean
    private CategoryService categoryService;

    @Nested
    @DisplayName("카테고리 등록")
    class CreateCategory {

        @Test
        @DisplayName("최상위 카테고리 등록 성공")
        void createCategory_success_root() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest(null, "전자제품");
            given(categoryService.createCategory(any(CategoryCreateRequest.class))).willReturn(1L);

            mockMvc.perform(post("/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1L));
        }

        @Test
        @DisplayName("하위 카테고리 등록 성공")
        void createCategory_success_child() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest(1L, "스마트폰");
            given(categoryService.createCategory(any(CategoryCreateRequest.class))).willReturn(2L);

            mockMvc.perform(post("/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(2L));
        }

        @Test
        @DisplayName("카테고리 이름 미입력 시 400 반환")
        void createCategory_blankName() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest(null, "");

            mockMvc.perform(post("/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("카테고리 이름 최대 길이 초과 시 400 반환")
        void createCategory_nameTooLong() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest(null, "a".repeat(51));

            mockMvc.perform(post("/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 부모 카테고리 지정 시 404 반환")
        void createCategory_parentNotFound() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest(99L, "스마트폰");
            given(categoryService.createCategory(any(CategoryCreateRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

            mockMvc.perform(post("/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(ErrorCode.CATEGORY_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("카테고리 이름 중복 시 409 반환")
        void createCategory_duplicateName() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest(null, "전자제품");
            given(categoryService.createCategory(any(CategoryCreateRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.DUPLICATE_CATEGORY_NAME));

            mockMvc.perform(post("/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_CATEGORY_NAME.getMessage()));
        }
    }
}
