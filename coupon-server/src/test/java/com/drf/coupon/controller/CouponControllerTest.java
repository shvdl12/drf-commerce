package com.drf.coupon.controller;

import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.DiscountType;
import com.drf.coupon.entity.MemberCouponStatus;
import com.drf.coupon.model.response.CouponIssueResponse;
import com.drf.coupon.model.response.MemberCouponListResponse;
import com.drf.coupon.service.CouponFacade;
import com.drf.coupon.service.CouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
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

    @MockitoBean
    private CouponFacade couponFacade;

    @Nested
    @DisplayName("쿠폰 발급")
    class IssueCoupon {

        @Test
        @DisplayName("발급 성공")
        void issueCoupon_success() throws Exception {
            given(couponFacade.issueCoupon(anyLong(), anyLong())).willReturn(new CouponIssueResponse(10L));

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
                    .given(couponFacade).issueCoupon(anyLong(), anyLong());

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
                    .given(couponFacade).issueCoupon(anyLong(), anyLong());

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
                    .given(couponFacade).issueCoupon(anyLong(), anyLong());

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
            MemberCouponListResponse response = MemberCouponListResponse.builder()
                    .memberCouponId(1L)
                    .couponName("신규 가입 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .maxDiscountAmount(null)
                    .minOrderAmount(10000)
                    .minOrderQuantity(1) // 적절한 값 넣어주세요
                    .applyType(ApplyType.ORDER)
                    .applyScope(null)
                    .applyTargetId(null)
                    .isUnlimited(false)
                    .maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 4, 30, 23, 59))
                    .status(MemberCouponStatus.UNUSED)
                    .usedAt(null)
                    .reservedAt(null)
                    .build();

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
}
