package com.drf.coupon.service;

import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import com.drf.coupon.discount.*;
import com.drf.coupon.entity.*;
import com.drf.coupon.model.response.CouponCalculateResponse;
import com.drf.coupon.model.response.CouponIssueResponse;
import com.drf.coupon.model.response.MemberCouponListResponse;
import com.drf.coupon.repository.CouponRepository;
import com.drf.coupon.repository.MemberCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private MemberCouponRepository memberCouponRepository;

    @Spy
    private DiscountPolicyRegistry discountPolicyRegistry = new DiscountPolicyRegistry(
            List.of(new FixedDiscountPolicy(), new RateDiscountPolicy())
    );

    @Spy
    private ApplyScopeRegistry applyScopeRegistry = new ApplyScopeRegistry(
            List.of(new AllApplyScope(), new CategoryApplyScope())
    );

    @BeforeEach
    void setUp() {
        couponService = new CouponService(
                couponRepository, memberCouponRepository,
                discountPolicyRegistry, applyScopeRegistry,
                List.of()
        );
    }

    private Coupon orderCoupon() {
        return Coupon.builder()
                .id(1L)
                .name("신규 가입 쿠폰")
                .discountType(DiscountType.FIXED)
                .discountValue(3000)
                .totalQuantity(100)
                .issuedQuantity(0)
                .minOrderAmount(10000)
                .applyType(ApplyType.ORDER)
                .maxIssuablePerMember(1)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .status(CouponStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("보유 쿠폰 목록 조회")
    class GetMemberCoupons {

        @Test
        @DisplayName("보유 쿠폰 목록 반환")
        void getMemberCoupons_success() {
            // given
            Coupon coupon = orderCoupon();
            MemberCoupon memberCoupon = MemberCoupon.builder()
                    .id(1L)
                    .coupon(coupon)
                    .memberId(1L)
                    .status(MemberCouponStatus.UNUSED)
                    .build();

            given(memberCouponRepository.findByMemberIdAndStatus(1L, MemberCouponStatus.UNUSED)).willReturn(List.of(memberCoupon));

            // when
            List<MemberCouponListResponse> result = couponService.getMemberCoupons(1L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).memberCouponId()).isEqualTo(1L);
            assertThat(result.get(0).couponName()).isEqualTo("신규 가입 쿠폰");
            assertThat(result.get(0).status()).isEqualTo(MemberCouponStatus.UNUSED);
        }

        @Test
        @DisplayName("보유 쿠폰이 없으면 빈 목록 반환")
        void getMemberCoupons_empty() {
            // given
            given(memberCouponRepository.findByMemberIdAndStatus(1L, MemberCouponStatus.UNUSED)).willReturn(List.of());

            // when
            List<MemberCouponListResponse> result = couponService.getMemberCoupons(1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("발급용 쿠폰 조회")
    class GetCouponForIssue {

        @Test
        @DisplayName("성공 - 쿠폰 반환")
        void getCouponForIssue_success() {
            // given
            given(couponRepository.findByIdAndStatus(1L, CouponStatus.ACTIVE)).willReturn(Optional.of(orderCoupon()));

            // when
            Coupon result = couponService.getCouponForIssue(1L, 1L);

            // then
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰이면 예외 발생")
        void getCouponForIssue_couponNotFound() {
            // given
            given(couponRepository.findByIdAndStatus(999L, CouponStatus.ACTIVE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> couponService.getCouponForIssue(999L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("회원 쿠폰 조회 (계산용)")
    class GetMemberCoupon {

        @Test
        @DisplayName("보유하지 않은 쿠폰이면 예외 발생")
        void getMemberCoupon_notFound() {
            // given
            given(memberCouponRepository.findByIdAndMemberIdAndStatus(999L, 1L, MemberCouponStatus.UNUSED))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> couponService.getMemberCoupon(999L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_COUPON_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("쿠폰 선점")
    class Reserve {

        @Test
        @DisplayName("선점 성공")
        void reserve_success() {
            given(memberCouponRepository.reserve(eq(1L), eq(1L), any())).willReturn(1);
            assertThatCode(() -> couponService.reserveCoupon(1L, 1L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("UNUSED 상태가 아니면 예외 발생")
        void reserve_failed() {
            given(memberCouponRepository.reserve(eq(1L), eq(1L), any())).willReturn(0);
            assertThatThrownBy(() -> couponService.reserveCoupon(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_RESERVE_FAILED);
        }
    }

    @Nested
    @DisplayName("쿠폰 선점 해제")
    class Release {

        @Test
        @DisplayName("선점 해제 성공")
        void release_success() {
            given(memberCouponRepository.release(1L, 1L)).willReturn(1);
            assertThatCode(() -> couponService.releaseCoupon(1L, 1L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("RESERVED 상태가 아니면 예외 발생")
        void release_failed() {
            given(memberCouponRepository.release(1L, 1L)).willReturn(0);
            assertThatThrownBy(() -> couponService.releaseCoupon(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_RELEASE_FAILED);
        }
    }

    @Nested
    @DisplayName("쿠폰 발급 처리")
    class Issue {

        @Test
        @DisplayName("발급 성공")
        void issue_success() {
            // given
            Coupon coupon = orderCoupon();
            MemberCoupon memberCoupon = MemberCoupon.builder()
                    .id(10L)
                    .coupon(coupon)
                    .memberId(1L)
                    .status(MemberCouponStatus.UNUSED)
                    .build();

            given(couponRepository.incrementIssuedQuantity(1L)).willReturn(1);
            given(memberCouponRepository.save(any(MemberCoupon.class))).willReturn(memberCoupon);

            // when
            CouponIssueResponse result = couponService.issueCoupon(coupon, 1L);

            // then
            assertThat(result.memberCouponId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("수량 소진 시 예외 발생")
        void issue_exhausted() {
            // given
            given(couponRepository.incrementIssuedQuantity(1L)).willReturn(0);

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(orderCoupon(), 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_EXHAUSTED);
        }
    }

    @Nested
    @DisplayName("쿠폰 할인가 계산")
    class Calculate {

        private MemberCoupon memberCoupon(Coupon coupon) {
            return MemberCoupon.builder()
                    .id(1L)
                    .coupon(coupon)
                    .memberId(1L)
                    .status(MemberCouponStatus.UNUSED)
                    .build();
        }

        @Test
        @DisplayName("정액 쿠폰 - 할인 금액 계산")
        void calculate_fixed() {
            // when
            CouponCalculateResponse result = couponService.calculate(memberCoupon(fixedCoupon()), 15000, null);

            // then
            assertThat(result.orderAmount()).isEqualTo(15000);
            assertThat(result.discountAmount()).isEqualTo(3000);
            assertThat(result.finalAmount()).isEqualTo(12000);
        }

        @Test
        @DisplayName("정률 쿠폰 (ORDER) - 할인 금액 계산")
        void calculate_rate_order() {
            // when
            CouponCalculateResponse result = couponService.calculate(memberCoupon(rateCoupon()), 20000, null);

            // then
            assertThat(result.discountAmount()).isEqualTo(2000);
            assertThat(result.finalAmount()).isEqualTo(18000);
        }

        @Test
        @DisplayName("정률 쿠폰 (ORDER) - 최대 할인 금액 적용")
        void calculate_rate_order_maxDiscount() {
            // when
            CouponCalculateResponse result = couponService.calculate(memberCoupon(rateCoupon()), 100000, null);

            // then
            assertThat(result.discountAmount()).isEqualTo(5000);
            assertThat(result.finalAmount()).isEqualTo(95000);
        }

        @Test
        @DisplayName("정률 쿠폰 - 최대 할인 금액 없으면 그대로 적용")
        void calculate_rate_noMaxDiscount() {
            // given
            Coupon coupon = Coupon.builder()
                    .id(3L)
                    .name("무제한 정률 쿠폰")
                    .discountType(DiscountType.RATE)
                    .discountValue(10)
                    .totalQuantity(100)
                    .issuedQuantity(1)
                    .minOrderAmount(10000)
                    .maxDiscountAmount(null)
                    .applyType(ApplyType.ORDER)
                    .maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .status(CouponStatus.ACTIVE)
                    .build();

            // when
            CouponCalculateResponse result = couponService.calculate(memberCoupon(coupon), 100000, null);

            // then
            assertThat(result.discountAmount()).isEqualTo(10000);
            assertThat(result.finalAmount()).isEqualTo(90000);
        }

        @Test
        @DisplayName("정률 쿠폰 (CATEGORY 스코프) - 카테고리 금액 기준 계산")
        void calculate_rate_category() {
            // when
            CouponCalculateResponse result = couponService.calculate(memberCoupon(rateCouponWithCategoryScope()), 20000, 12000);

            // then
            assertThat(result.discountAmount()).isEqualTo(1200);
            assertThat(result.finalAmount()).isEqualTo(18800);
        }

        @Test
        @DisplayName("FIXED 할인가가 주문 금액보다 크면 finalAmount는 0")
        void calculate_fixed_discountExceedsOrderAmount() {
            // given
            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .name("대형 정액 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(20000)
                    .totalQuantity(100)
                    .issuedQuantity(1)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ORDER)
                    .maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .status(CouponStatus.ACTIVE)
                    .build();

            // when
            CouponCalculateResponse result = couponService.calculate(memberCoupon(coupon), 12000, null);

            // then
            assertThat(result.discountAmount()).isEqualTo(20000);
            assertThat(result.finalAmount()).isEqualTo(0);
        }

        private Coupon fixedCoupon() {
            return Coupon.builder()
                    .id(1L)
                    .name("정액 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .issuedQuantity(1)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ORDER)
                    .maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .status(CouponStatus.ACTIVE)
                    .build();
        }

        private Coupon rateCoupon() {
            return Coupon.builder()
                    .id(2L)
                    .name("정률 쿠폰")
                    .discountType(DiscountType.RATE)
                    .discountValue(10)
                    .totalQuantity(100)
                    .issuedQuantity(1)
                    .minOrderAmount(10000)
                    .maxDiscountAmount(5000)
                    .applyType(ApplyType.ORDER)
                    .maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .status(CouponStatus.ACTIVE)
                    .build();
        }

        private Coupon rateCouponWithCategoryScope() {
            return Coupon.builder()
                    .id(2L)
                    .name("카테고리 정률 쿠폰")
                    .discountType(DiscountType.RATE)
                    .discountValue(10)
                    .totalQuantity(100)
                    .issuedQuantity(1)
                    .minOrderAmount(10000)
                    .maxDiscountAmount(5000)
                    .applyType(ApplyType.PRODUCT)
                    .applyScope(ApplyScope.CATEGORY)
                    .applyTargetId(1L)
                    .maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .status(CouponStatus.ACTIVE)
                    .build();
        }
    }
}
