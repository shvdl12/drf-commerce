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
    @DisplayName("쿠폰 발급")
    class IssueCoupon {

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
        @DisplayName("발급 성공")
        void issueCoupon_success() {
            // given
            Coupon coupon = activeCoupon();
            MemberCoupon memberCoupon = MemberCoupon.builder()
                    .id(10L)
                    .coupon(coupon)
                    .memberId(1L)
                    .status(MemberCouponStatus.UNUSED)
                    .build();

            given(couponRepository.findByIdAndStatus(1L, CouponStatus.ACTIVE)).willReturn(Optional.of(coupon));
            given(memberCouponRepository.existsByMemberIdAndCouponId(1L, 1L)).willReturn(false);
            given(couponRepository.incrementIssuedQuantity(1L)).willReturn(1);
            given(memberCouponRepository.save(any(MemberCoupon.class))).willReturn(memberCoupon);

            // when
            CouponIssueResponse result = couponService.issueCoupon(1L, 1L);

            // then
            assertThat(result.memberCouponId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰이면 예외 발생")
        void issueCoupon_couponNotFound() {
            // given
            given(couponRepository.findByIdAndStatus(999L, CouponStatus.ACTIVE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_NOT_FOUND);
        }

        @Test
        @DisplayName("유효기간이 아니면 예외 발생")
        void issueCoupon_notAvailable() {
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
            assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰이면 예외 발생")
        void issueCoupon_alreadyIssued() {
            // given
            given(couponRepository.findByIdAndStatus(1L, CouponStatus.ACTIVE)).willReturn(Optional.of(activeCoupon()));
            given(memberCouponRepository.existsByMemberIdAndCouponId(1L, 1L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        @Test
        @DisplayName("수량 소진 시 예외 발생")
        void issueCoupon_exhausted() {
            // given
            given(couponRepository.findByIdAndStatus(1L, CouponStatus.ACTIVE)).willReturn(Optional.of(activeCoupon()));
            given(memberCouponRepository.existsByMemberIdAndCouponId(1L, 1L)).willReturn(false);
            given(couponRepository.incrementIssuedQuantity(1L)).willReturn(0);

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_EXHAUSTED);
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰이면 예외 발생 (유니크 제약 조건)")
        void issueCoupon_alreadyIssued_UniqueError() {
            // given
            Coupon coupon = activeCoupon();

            given(couponRepository.findByIdAndStatus(1L, CouponStatus.ACTIVE)).willReturn(Optional.of(coupon));
            given(memberCouponRepository.existsByMemberIdAndCouponId(1L, 1L)).willReturn(false);
            given(couponRepository.incrementIssuedQuantity(1L)).willReturn(1);
            given(memberCouponRepository.save(any(MemberCoupon.class)))
                    .willThrow(new DataIntegrityViolationException("쿠폰 중복"));

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }

    @Nested
    @DisplayName("쿠폰 적용 가격 계산")
    class CalculateCoupon {

        private MemberCoupon unusedMemberCoupon(Coupon coupon) {
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
        void calculateCoupon_fixed() {
            // given
            Coupon coupon = fixedCoupon();
            given(memberCouponRepository.findByIdAndMemberIdAndStatus(1L, 1L, MemberCouponStatus.UNUSED))
                    .willReturn(Optional.of(unusedMemberCoupon(coupon)));

            // when
            CouponCalculateResponse result = couponService.calculateCoupon(1L, 1L, 15000, null);

            // then
            assertThat(result.orderAmount()).isEqualTo(15000);
            assertThat(result.discountAmount()).isEqualTo(3000);
            assertThat(result.finalAmount()).isEqualTo(12000);
        }

        @Test
        @DisplayName("정률 쿠폰 (ALL) - 할인 금액 계산")
        void calculateCoupon_rate_all() {
            // given
            Coupon coupon = rateCoupon(ApplyType.ALL);
            given(memberCouponRepository.findByIdAndMemberIdAndStatus(1L, 1L, MemberCouponStatus.UNUSED))
                    .willReturn(Optional.of(unusedMemberCoupon(coupon)));

            // when
            CouponCalculateResponse result = couponService.calculateCoupon(1L, 1L, 20000, null);

            // then
            assertThat(result.discountAmount()).isEqualTo(2000); // 20000 * 10% = 2000
            assertThat(result.finalAmount()).isEqualTo(18000);
        }

        @Test
        @DisplayName("정률 쿠폰 (ALL) - 최대 할인 금액 적용")
        void calculateCoupon_rate_all_maxDiscount() {
            // given
            Coupon coupon = rateCoupon(ApplyType.ALL);
            given(memberCouponRepository.findByIdAndMemberIdAndStatus(1L, 1L, MemberCouponStatus.UNUSED))
                    .willReturn(Optional.of(unusedMemberCoupon(coupon)));

            // when
            CouponCalculateResponse result = couponService.calculateCoupon(1L, 1L, 100000, null);

            // then
            assertThat(result.discountAmount()).isEqualTo(5000); // 100000 * 10% = 10000 → cap 5000
            assertThat(result.finalAmount()).isEqualTo(95000);
        }

        @Test
        @DisplayName("정률 쿠폰 - 최대 할인 금액 없으면 그대로 적용")
        void calculateCoupon_rate_noMaxDiscount() {
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

            given(memberCouponRepository.findByIdAndMemberIdAndStatus(1L, 1L, MemberCouponStatus.UNUSED))
                    .willReturn(Optional.of(unusedMemberCoupon(coupon)));

            // when
            CouponCalculateResponse result = couponService.calculateCoupon(1L, 1L, 100000, null);

            // then
            assertThat(result.discountAmount()).isEqualTo(10000); // 100000 * 10% = 10000, 상한 없음
            assertThat(result.finalAmount()).isEqualTo(90000);
        }

        @Test
        @DisplayName("정률 쿠폰 (CATEGORY) - 카테고리 금액 기준 계산")
        void calculateCoupon_rate_category() {
            // given
            Coupon coupon = rateCoupon(ApplyType.CATEGORY);
            given(memberCouponRepository.findByIdAndMemberIdAndStatus(1L, 1L, MemberCouponStatus.UNUSED))
                    .willReturn(Optional.of(unusedMemberCoupon(coupon)));

            // when
            CouponCalculateResponse result = couponService.calculateCoupon(1L, 1L, 20000, 12000);

            // then
            assertThat(result.discountAmount()).isEqualTo(1200); // 12000 * 10% = 1200
            assertThat(result.finalAmount()).isEqualTo(18800);
        }

        @Test
        @DisplayName("FIXED 할인가가 주문 금액보다 크면 finalAmount는 0")
        void calculateCoupon_fixed_discountExceedsOrderAmount() {
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

            given(memberCouponRepository.findByIdAndMemberIdAndStatus(1L, 1L, MemberCouponStatus.UNUSED))
                    .willReturn(Optional.of(unusedMemberCoupon(coupon)));

            // when
            CouponCalculateResponse result = couponService.calculateCoupon(1L, 1L, 12000, null);

            // then
            assertThat(result.discountAmount()).isEqualTo(20000);
            assertThat(result.finalAmount()).isEqualTo(0);
        }

        @Test
        @DisplayName("CATEGORY 쿠폰인데 categoryAmount가 없으면 예외 발생")
        void calculateCoupon_category_missingCategoryAmount() {
            // given
            Coupon coupon = rateCoupon(ApplyType.CATEGORY);
            given(memberCouponRepository.findByIdAndMemberIdAndStatus(1L, 1L, MemberCouponStatus.UNUSED))
                    .willReturn(Optional.of(unusedMemberCoupon(coupon)));

            // when & then
            assertThatThrownBy(() -> couponService.calculateCoupon(1L, 1L, 15000, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CATEGORY_AMOUNT_REQUIRED);
        }

        @Test
        @DisplayName("CATEGORY + FIXED 쿠폰인데 categoryAmount가 없으면 예외 발생")
        void calculateCoupon_categoryFixed_missingCategoryAmount() {
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

            given(memberCouponRepository.findByIdAndMemberIdAndStatus(1L, 1L, MemberCouponStatus.UNUSED))
                    .willReturn(Optional.of(unusedMemberCoupon(coupon)));

            // when & then
            assertThatThrownBy(() -> couponService.calculateCoupon(1L, 1L, 15000, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CATEGORY_AMOUNT_REQUIRED);
        }

        @Test
        @DisplayName("보유하지 않은 쿠폰이면 예외 발생")
        void calculateCoupon_memberCouponNotFound() {
            // given
            given(memberCouponRepository.findByIdAndMemberIdAndStatus(999L, 1L, MemberCouponStatus.UNUSED))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> couponService.calculateCoupon(1L, 999L, 15000, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_COUPON_NOT_FOUND);
        }

        @Test
        @DisplayName("유효기간이 아니면 예외 발생")
        void calculateCoupon_notAvailable() {
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

            given(memberCouponRepository.findByIdAndMemberIdAndStatus(1L, 1L, MemberCouponStatus.UNUSED))
                    .willReturn(Optional.of(unusedMemberCoupon(expiredCoupon)));

            // when & then
            assertThatThrownBy(() -> couponService.calculateCoupon(1L, 1L, 15000, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("최소 주문 금액 미충족 시 예외 발생")
        void calculateCoupon_minOrderAmountNotMet() {
            // given
            Coupon coupon = fixedCoupon();
            given(memberCouponRepository.findByIdAndMemberIdAndStatus(1L, 1L, MemberCouponStatus.UNUSED))
                    .willReturn(Optional.of(unusedMemberCoupon(coupon)));

            // when & then
            assertThatThrownBy(() -> couponService.calculateCoupon(1L, 1L, 5000, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
        }
    }
}
