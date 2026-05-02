package com.drf.inventory.controller;

import com.drf.inventory.model.request.StockCreateRequest;
import com.drf.inventory.model.response.StockResponse;
import com.drf.inventory.service.AdminStockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AdminStockController.class)
class AdminStockControllerTest extends BaseControllerTest {

    @MockitoBean
    private AdminStockService adminStockService;

    @Nested
    @DisplayName("재고 조회")
    class GetStocks {
        @Test
        @DisplayName("확정 재고 조회 성공 - 200 OK와 목록을 반환한다")
        void getConfirmedStocks_success() throws Exception {
            // given
            List<Long> productIds = List.of(1L, 2L);
            List<StockResponse> responses = List.of(
                    new StockResponse(1L, 100L),
                    new StockResponse(2L, 200L)
            );
            given(adminStockService.getStocks(productIds)).willReturn(responses);

            // when & then
            mockMvc.perform(get("/admin/stocks/confirmed")
                            .param("productIds", "1,2")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].productId").value(1L))
                    .andExpect(jsonPath("$.data[0].stock").value(100L))
                    .andExpect(jsonPath("$.data[1].productId").value(2L))
                    .andExpect(jsonPath("$.data[1].stock").value(200L));
        }

        @Test
        @DisplayName("상품 ID 목록 누락 시 400 Bad Request를 반환한다")
        void getConfirmedStocks_fail_emptyProductIds() throws Exception {
            // when & then
            mockMvc.perform(get("/admin/stocks/confirmed")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("재고 등록")
    class CreateStock {
        @Test
        @DisplayName("재고 등록 성공 - 200 OK를 반환한다")
        void createStock_success() throws Exception {
            // given
            StockCreateRequest request = new StockCreateRequest(1L, 100L);
            willDoNothing().given(adminStockService).createStock(any());

            // when & then
            mockMvc.perform(post("/admin/stocks")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("필수 파라미터 누락 시 400 Bad Request를 반환한다")
        void createStock_fail_invalidRequest() throws Exception {
            // given
            StockCreateRequest request = new StockCreateRequest(null, -1L);

            // when & then
            mockMvc.perform(post("/admin/stocks")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
