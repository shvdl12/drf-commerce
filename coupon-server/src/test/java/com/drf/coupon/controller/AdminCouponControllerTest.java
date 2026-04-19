package com.drf.coupon.controller;

import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import com.drf.coupon.entity.ApplyScope;
import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.CouponStatus;
import com.drf.coupon.entity.DiscountType;
import com.drf.coupon.model.request.CouponCreateRequest;
import com.drf.coupon.model.request.CouponUpdateRequest;
import com.drf.coupon.model.response.CouponListResponse;
import com.drf.coupon.service.CouponAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
                    .minOrderQuantity(0)
                    .applyType(ApplyType.ORDER)
                    .applyScope(ApplyScope.ALL)
                    .maxIssuablePerMember(1)
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
                    .minOrderQuantity(0)
                    .applyType(ApplyType.ORDER)
                    .applyScope(ApplyScope.ALL)
                    .maxIssuablePerMember(1)
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
                    .minOrderQuantity(0)
                    .applyType(ApplyType.ORDER)
                    .applyScope(ApplyScope.ALL)
                    .maxIssuablePerMember(1)
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

    @Nested
    @DisplayName("쿠폰 목록 조회")
    class GetCoupons {

        @Test
        @DisplayName("조회 성공")
        void getCoupons_success() throws Exception {
            CouponListResponse response = CouponListResponse.builder()
                    .couponId(1L)
                    .couponName("신규 가입 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .maxDiscountAmount(null)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ORDER)
                    .applyTargetId(null)
                    .validFrom(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 4, 30, 23, 59))
                    .totalQuantity(100)
                    .issuedQuantity(1)
                    .maxIssuablePerMember(1)
                    .status(CouponStatus.ACTIVE)
                    .build();

            given(couponAdminService.getCoupons()).willReturn(List.of(response));

            mockMvc.perform(get("/admin/coupons")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].couponId").value(1L))
                    .andExpect(jsonPath("$.data[0].totalQuantity").value(100))
                    .andExpect(jsonPath("$.data[0].issuedQuantity").value(1));
        }

        @Test
        @DisplayName("쿠폰이 없으면 빈 목록 반환")
        void getCoupons_empty() throws Exception {
            given(couponAdminService.getCoupons()).willReturn(List.of());

            mockMvc.perform(get("/admin/coupons")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("쿠폰 수정")
    class UpdateCoupon {

        @Test
        @DisplayName("수정 성공")
        void updateCoupon_success() throws Exception {
            CouponUpdateRequest request = CouponUpdateRequest.builder()
                    .name("수정된 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(5000)
                    .totalQuantity(200)
                    .minOrderAmount(20000)
                    .minOrderQuantity(0)
                    .applyType(ApplyType.ORDER)
                    .applyScope(ApplyScope.ALL)
                    .maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.of(2026, 5, 1, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 5, 31, 23, 59))
                    .build();

            willDoNothing().given(couponAdminService).updateCoupon(anyLong(), any(CouponUpdateRequest.class));

            mockMvc.perform(put("/admin/coupons/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "ADMIN")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 수정 시 404 반환")
        void updateCoupon_notFound() throws Exception {
            CouponUpdateRequest request = CouponUpdateRequest.builder()
                    .name("수정된 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(5000)
                    .totalQuantity(200)
                    .minOrderAmount(20000)
                    .minOrderQuantity(0)
                    .applyType(ApplyType.ORDER)
                    .applyScope(ApplyScope.ALL)
                    .maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.of(2026, 5, 1, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 5, 31, 23, 59))
                    .build();

            willThrow(new BusinessException(ErrorCode.COUPON_NOT_FOUND))
                    .given(couponAdminService).updateCoupon(anyLong(), any(CouponUpdateRequest.class));

            mockMvc.perform(put("/admin/coupons/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "ADMIN")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(ErrorCode.COUPON_NOT_FOUND.getMessage()));
        }
    }

    @Nested
    @DisplayName("쿠폰 삭제")
    class DeleteCoupon {

        @Test
        @DisplayName("삭제 성공")
        void deleteCoupon_success() throws Exception {
            willDoNothing().given(couponAdminService).deleteCoupon(anyLong());

            mockMvc.perform(delete("/admin/coupons/1")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 삭제 시 404 반환")
        void deleteCoupon_notFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.COUPON_NOT_FOUND))
                    .given(couponAdminService).deleteCoupon(anyLong());

            mockMvc.perform(delete("/admin/coupons/999")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(ErrorCode.COUPON_NOT_FOUND.getMessage()));
        }
    }
}
