package com.drf.product.controller;

import com.drf.common.exception.BusinessException;
import com.drf.product.common.exception.ErrorCode;
import com.drf.product.model.request.CategoryCreateRequest;
import com.drf.product.model.request.CategoryUpdateRequest;
import com.drf.product.model.response.CategoryTreeResponse;
import com.drf.product.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Nested
    @DisplayName("카테고리 계층 전체 조회")
    class GetCategories {

        @Test
        @DisplayName("카테고리 트리 조회 성공")
        void getCategories_success() throws Exception {
            CategoryTreeResponse child = new CategoryTreeResponse(2L, "스마트폰", List.of());
            CategoryTreeResponse root = new CategoryTreeResponse(1L, "전자제품", List.of(child));
            given(categoryService.getCategories()).willReturn(List.of(root));

            mockMvc.perform(get("/categories")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(1L))
                    .andExpect(jsonPath("$.data[0].name").value("전자제품"))
                    .andExpect(jsonPath("$.data[0].children[0].name").value("스마트폰"));
        }
    }

    @Nested
    @DisplayName("카테고리 수정")
    class UpdateCategory {

        @Test
        @DisplayName("카테고리 이름 수정 성공 - 204 반환")
        void updateCategory_success() throws Exception {
            CategoryUpdateRequest request = new CategoryUpdateRequest("태블릿");

            mockMvc.perform(post("/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("이름 미입력 시 400 반환")
        void updateCategory_blankName() throws Exception {
            CategoryUpdateRequest request = new CategoryUpdateRequest("");

            mockMvc.perform(post("/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("이름 최대 길이 초과 시 400 반환")
        void updateCategory_nameTooLong() throws Exception {
            CategoryUpdateRequest request = new CategoryUpdateRequest("a".repeat(51));

            mockMvc.perform(post("/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 수정 시 404 반환")
        void updateCategory_notFound() throws Exception {
            CategoryUpdateRequest request = new CategoryUpdateRequest("태블릿");
            willThrow(new BusinessException(ErrorCode.CATEGORY_NOT_FOUND))
                    .given(categoryService).updateCategory(anyLong(), any(CategoryUpdateRequest.class));

            mockMvc.perform(post("/categories/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(ErrorCode.CATEGORY_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("이름 중복 시 409 반환")
        void updateCategory_duplicateName() throws Exception {
            CategoryUpdateRequest request = new CategoryUpdateRequest("전자제품");
            willThrow(new BusinessException(ErrorCode.DUPLICATE_CATEGORY_NAME))
                    .given(categoryService).updateCategory(anyLong(), any(CategoryUpdateRequest.class));

            mockMvc.perform(post("/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_CATEGORY_NAME.getMessage()));
        }
    }
}
