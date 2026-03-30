package com.drf.coupon.controller;

import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.DiscountType;
import com.drf.coupon.model.request.CouponCreateRequest;
import com.drf.coupon.service.CouponAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCouponController.class)
class AdminCouponControllerTest extends BaseControllerTest {

    @MockitoBean
    private CouponAdminService couponAdminService;

    @Nested
    @DisplayName("쿠폰 등록")
    class CreateCoupon {

        @Test
        @DisplayName("등록 성공")
        void createCoupon_success() throws Exception {
            CouponCreateRequest request = CouponCreateRequest.builder()
                    .name("신규 가입 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ALL)
                    .validFrom(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 4, 30, 23, 59))
                    .build();

            given(couponAdminService.createCoupon(any(CouponCreateRequest.class))).willReturn(1L);

            mockMvc.perform(post("/admin/coupons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "ADMIN")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1L));
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 반환")
        void createCoupon_missingRequiredField() throws Exception {
            CouponCreateRequest request = CouponCreateRequest.builder()
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ALL)
                    .validFrom(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 4, 30, 23, 59))
                    .build();

            mockMvc.perform(post("/admin/coupons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "ADMIN")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("유효하지 않은 유효기간으로 등록 시 400 반환")
        void createCoupon_invalidValidDateRange() throws Exception {
            CouponCreateRequest request = CouponCreateRequest.builder()
                    .name("신규 가입 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ALL)
                    .validFrom(LocalDateTime.of(2026, 4, 30, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .build();

            willThrow(new BusinessException(ErrorCode.INVALID_VALID_DATE_RANGE))
                    .given(couponAdminService).createCoupon(any(CouponCreateRequest.class));

            mockMvc.perform(post("/admin/coupons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "ADMIN")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_VALID_DATE_RANGE.getMessage()));
        }
    }
}
