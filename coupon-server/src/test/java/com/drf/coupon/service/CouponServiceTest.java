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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
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

    @Nested
    @DisplayName("보유 쿠폰 목록 조회")
    class GetMemberCoupons {

        @Test
        @DisplayName("보유 쿠폰 목록 반환")
        void getMemberCoupons_success() {
            // given
            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .name("신규 가입 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .issuedQuantity(1)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ALL)
                    .validFrom(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 4, 30, 23, 59))
                    .status(CouponStatus.ACTIVE)
                    .build();

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
    @DisplayName("쿠폰 발급 검증")
    class ValidateAndGetCoupon {

        private Coupon activeCoupon() {
            return Coupon.builder()
                    .id(1L)
                    .name("신규 가입 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .issuedQuantity(0)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ALL)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .status(CouponStatus.ACTIVE)
                    .build();
        }

        @Test
        @DisplayName("검증 성공 - 쿠폰 반환")
        void validateAndGetCoupon_success() {
            // given
            given(couponRepository.findByIdAndStatus(1L, CouponStatus.ACTIVE)).willReturn(Optional.of(activeCoupon()));
            given(memberCouponRepository.existsByMemberIdAndCouponId(1L, 1L)).willReturn(false);

            // when
            Coupon result = couponService.getCouponForIssue(1L, 1L);

            // then
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰이면 예외 발생")
        void validateAndGetCoupon_couponNotFound() {
            // given
            given(couponRepository.findByIdAndStatus(999L, CouponStatus.ACTIVE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> couponService.getCouponForIssue(999L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_NOT_FOUND);
        }

        @Test
        @DisplayName("유효기간이 아니면 예외 발생")
        void validateAndGetCoupon_notAvailable() {
            // given
            Coupon expiredCoupon = Coupon.builder()
                    .id(1L)
                    .name("만료 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .issuedQuantity(0)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ALL)
                    .validFrom(LocalDateTime.now().minusDays(30))
                    .validUntil(LocalDateTime.now().minusDays(1))
                    .status(CouponStatus.ACTIVE)
                    .build();

            given(couponRepository.findByIdAndStatus(1L, CouponStatus.ACTIVE)).willReturn(Optional.of(expiredCoupon));

            // when & then
            assertThatThrownBy(() -> couponService.getCouponForIssue(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰이면 예외 발생")
        void validateAndGetCoupon_alreadyIssued() {
            // given
            given(couponRepository.findByIdAndStatus(1L, CouponStatus.ACTIVE)).willReturn(Optional.of(activeCoupon()));
            given(memberCouponRepository.existsByMemberIdAndCouponId(1L, 1L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> couponService.getCouponForIssue(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }

    @Nested
    @DisplayName("쿠폰 발급 처리")
    class Issue {

        private Coupon coupon() {
            return Coupon.builder()
                    .id(1L)
                    .name("신규 가입 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .issuedQuantity(0)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ALL)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .status(CouponStatus.ACTIVE)
                    .build();
        }

        @Test
        @DisplayName("발급 성공")
        void issue_success() {
            // given
            Coupon coupon = coupon();
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
            assertThatThrownBy(() -> couponService.issueCoupon(coupon(), 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_EXHAUSTED);
        }

        @Test
        @DisplayName("유니크 제약 조건 위반 시 중복 발급 예외 발생")
        void issue_alreadyIssued_uniqueError() {
            // given
            given(couponRepository.incrementIssuedQuantity(1L)).willReturn(1);
            given(memberCouponRepository.save(any(MemberCoupon.class)))
                    .willThrow(new DataIntegrityViolationException("쿠폰 중복"));

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(coupon(), 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }

    @Nested
    @DisplayName("회원 쿠폰 조회 (계산용)")
    class GetMemberCouponForCalculate {

        @Test
        @DisplayName("보유하지 않은 쿠폰이면 예외 발생")
        void getMemberCouponForCalculate_notFound() {
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

        private Coupon fixedCoupon() {
            return Coupon.builder()
                    .id(1L)
                    .name("정액 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .issuedQuantity(1)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ALL)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .status(CouponStatus.ACTIVE)
                    .build();
        }

        private Coupon rateCoupon(ApplyType applyType) {
            return Coupon.builder()
                    .id(2L)
                    .name("정률 쿠폰")
                    .discountType(DiscountType.RATE)
                    .discountValue(10)
                    .totalQuantity(100)
                    .issuedQuantity(1)
                    .minOrderAmount(10000)
                    .maxDiscountAmount(5000)
                    .applyType(applyType)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .status(CouponStatus.ACTIVE)
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
        @DisplayName("정률 쿠폰 (ALL) - 할인 금액 계산")
        void calculate_rate_all() {
            // when
            CouponCalculateResponse result = couponService.calculate(memberCoupon(rateCoupon(ApplyType.ALL)), 20000, null);

            // then
            assertThat(result.discountAmount()).isEqualTo(2000);
            assertThat(result.finalAmount()).isEqualTo(18000);
        }

        @Test
        @DisplayName("정률 쿠폰 (ALL) - 최대 할인 금액 적용")
        void calculate_rate_all_maxDiscount() {
            // when
            CouponCalculateResponse result = couponService.calculate(memberCoupon(rateCoupon(ApplyType.ALL)), 100000, null);

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
                    .applyType(ApplyType.ALL)
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
        @DisplayName("정률 쿠폰 (CATEGORY) - 카테고리 금액 기준 계산")
        void calculate_rate_category() {
            // when
            CouponCalculateResponse result = couponService.calculate(memberCoupon(rateCoupon(ApplyType.CATEGORY)), 20000, 12000);

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
                    .applyType(ApplyType.ALL)
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

        @Test
        @DisplayName("유효기간이 아니면 예외 발생")
        void calculate_notAvailable() {
            // given
            Coupon expiredCoupon = Coupon.builder()
                    .id(1L)
                    .name("만료 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .issuedQuantity(1)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ALL)
                    .validFrom(LocalDateTime.now().minusDays(30))
                    .validUntil(LocalDateTime.now().minusDays(1))
                    .status(CouponStatus.ACTIVE)
                    .build();

            // when & then
            assertThatThrownBy(() -> couponService.calculate(memberCoupon(expiredCoupon), 15000, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("최소 주문 금액 미충족 시 예외 발생")
        void calculate_minOrderAmountNotMet() {
            // when & then
            assertThatThrownBy(() -> couponService.calculate(memberCoupon(fixedCoupon()), 5000, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
        }

        @Test
        @DisplayName("CATEGORY 쿠폰인데 categoryAmount가 없으면 예외 발생")
        void calculate_category_missingCategoryAmount() {
            // when & then
            assertThatThrownBy(() -> couponService.calculate(memberCoupon(rateCoupon(ApplyType.CATEGORY)), 15000, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CATEGORY_AMOUNT_REQUIRED);
        }

        @Test
        @DisplayName("CATEGORY + FIXED 쿠폰인데 categoryAmount가 없으면 예외 발생")
        void calculate_categoryFixed_missingCategoryAmount() {
            // given
            Coupon coupon = Coupon.builder()
                    .id(3L)
                    .name("카테고리 정액 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .issuedQuantity(1)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.CATEGORY)
                    .applyTargetId(10L)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .status(CouponStatus.ACTIVE)
                    .build();

            // when & then
            assertThatThrownBy(() -> couponService.calculate(memberCoupon(coupon), 15000, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CATEGORY_AMOUNT_REQUIRED);
        }
    }
}
