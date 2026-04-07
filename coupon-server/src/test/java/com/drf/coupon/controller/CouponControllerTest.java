package com.drf.coupon.controller;

import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.DiscountType;
import com.drf.coupon.entity.MemberCouponStatus;
import com.drf.coupon.model.response.CouponCalculateResponse;
import com.drf.coupon.model.response.CouponIssueResponse;
import com.drf.coupon.model.response.MemberCouponListResponse;
import com.drf.coupon.service.CouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
class CouponControllerTest extends BaseControllerTest {

    @MockitoBean
    private CouponService couponService;

    @Nested
    @DisplayName("쿠폰 발급")
    class IssueCoupon {

        @Test
        @DisplayName("발급 성공")
        void issueCoupon_success() throws Exception {
            given(couponService.issueCoupon(anyLong(), anyLong())).willReturn(new CouponIssueResponse(10L));

            mockMvc.perform(post("/members/me/coupons/1")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.memberCouponId").value(10L));
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰이면 404 반환")
        void issueCoupon_couponNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.COUPON_NOT_FOUND))
                    .given(couponService).issueCoupon(anyLong(), anyLong());

            mockMvc.perform(post("/members/me/coupons/999")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(ErrorCode.COUPON_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰이면 409 반환")
        void issueCoupon_alreadyIssued() throws Exception {
            willThrow(new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED))
                    .given(couponService).issueCoupon(anyLong(), anyLong());

            mockMvc.perform(post("/members/me/coupons/1")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(ErrorCode.COUPON_ALREADY_ISSUED.getMessage()));
        }

        @Test
        @DisplayName("수량 소진 시 409 반환")
        void issueCoupon_exhausted() throws Exception {
            willThrow(new BusinessException(ErrorCode.COUPON_EXHAUSTED))
                    .given(couponService).issueCoupon(anyLong(), anyLong());

            mockMvc.perform(post("/members/me/coupons/1")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(ErrorCode.COUPON_EXHAUSTED.getMessage()));
        }
    }

    @Nested
    @DisplayName("보유 쿠폰 목록 조회")
    class GetMemberCoupons {

        @Test
        @DisplayName("조회 성공")
        void getMemberCoupons_success() throws Exception {
            MemberCouponListResponse response = new MemberCouponListResponse(
                    1L, "신규 가입 쿠폰", DiscountType.FIXED, 3000, null, 10000,
                    ApplyType.ALL, null,
                    LocalDateTime.of(2026, 4, 1, 0, 0), LocalDateTime.of(2026, 4, 30, 23, 59),
                    MemberCouponStatus.UNUSED
            );

            given(couponService.getMemberCoupons(anyLong())).willReturn(List.of(response));

            mockMvc.perform(get("/members/me/coupons")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].memberCouponId").value(1L))
                    .andExpect(jsonPath("$.data[0].couponName").value("신규 가입 쿠폰"))
                    .andExpect(jsonPath("$.data[0].status").value("UNUSED"));
        }

        @Test
        @DisplayName("보유 쿠폰이 없으면 빈 목록 반환")
        void getMemberCoupons_empty() throws Exception {
            given(couponService.getMemberCoupons(anyLong())).willReturn(List.of());

            mockMvc.perform(get("/members/me/coupons")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("쿠폰 적용 가격 계산")
    class CalculateCoupon {

        @Test
        @DisplayName("계산 성공")
        void calculateCoupon_success() throws Exception {
            given(couponService.calculateCoupon(anyLong(), anyLong(), anyInt(), any()))
                    .willReturn(new CouponCalculateResponse(15000, 3000, 12000));

            mockMvc.perform(get("/members/me/coupons/1/calculate")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .param("orderAmount", "15000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.orderAmount").value(15000))
                    .andExpect(jsonPath("$.data.discountAmount").value(3000))
                    .andExpect(jsonPath("$.data.finalAmount").value(12000));
        }

        @Test
        @DisplayName("보유하지 않은 쿠폰이면 404 반환")
        void calculateCoupon_memberCouponNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.MEMBER_COUPON_NOT_FOUND))
                    .given(couponService).calculateCoupon(anyLong(), anyLong(), anyInt(), any());

            mockMvc.perform(get("/members/me/coupons/999/calculate")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .param("orderAmount", "15000"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(ErrorCode.MEMBER_COUPON_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("최소 주문 금액 미충족 시 400 반환")
        void calculateCoupon_minOrderAmountNotMet() throws Exception {
            willThrow(new BusinessException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET))
                    .given(couponService).calculateCoupon(anyLong(), anyLong(), anyInt(), any());

            mockMvc.perform(get("/members/me/coupons/1/calculate")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .param("orderAmount", "5000"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET.getMessage()));
        }

        @Test
        @DisplayName("카테고리 쿠폰인데 categoryAmount가 없으면 400 반환")
        void calculateCoupon_categoryAmountRequired() throws Exception {
            willThrow(new BusinessException(ErrorCode.CATEGORY_AMOUNT_REQUIRED))
                    .given(couponService).calculateCoupon(anyLong(), anyLong(), anyInt(), any());

            mockMvc.perform(get("/members/me/coupons/1/calculate")
                            .header("X-User-Id", 1)
                            .header("X-User-Role", "USER")
                            .param("orderAmount", "15000"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(ErrorCode.CATEGORY_AMOUNT_REQUIRED.getMessage()));
        }
    }
}
