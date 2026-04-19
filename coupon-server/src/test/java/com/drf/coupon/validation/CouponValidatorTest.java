package com.drf.coupon.validation;

import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import com.drf.coupon.entity.*;
import com.drf.coupon.repository.MemberCouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CouponValidatorTest {

    @Mock
    private MemberCouponRepository memberCouponRepository;

    @InjectMocks
    private CouponAlreadyIssuedValidator alreadyIssuedValidator;

    private Coupon coupon(int minOrderAmount, int maxIssuablePerMember) {
        return Coupon.builder()
                .id(1L)
                .name("테스트 쿠폰")
                .discountType(DiscountType.FIXED)
                .discountValue(3000)
                .totalQuantity(100)
                .issuedQuantity(0)
                .minOrderAmount(minOrderAmount)
                .applyType(ApplyType.ORDER)
                .maxIssuablePerMember(maxIssuablePerMember)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .status(CouponStatus.ACTIVE)
                .build();
    }

    private Coupon couponWithCategoryScope(int minOrderAmount) {
        return Coupon.builder()
                .id(1L)
                .name("카테고리 쿠폰")
                .discountType(DiscountType.FIXED)
                .discountValue(3000)
                .totalQuantity(100)
                .issuedQuantity(0)
                .minOrderAmount(minOrderAmount)
                .applyType(ApplyType.PRODUCT)
                .applyScope(ApplyScope.CATEGORY)
                .applyTargetId(1L)
                .maxIssuablePerMember(1)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .status(CouponStatus.ACTIVE)
                .build();
    }

    private Coupon expiredCoupon() {
        return Coupon.builder()
                .id(1L)
                .name("만료 쿠폰")
                .discountType(DiscountType.FIXED)
                .discountValue(3000)
                .totalQuantity(100)
                .issuedQuantity(0)
                .minOrderAmount(10000)
                .applyType(ApplyType.ORDER)
                .maxIssuablePerMember(1)
                .validFrom(LocalDateTime.now().minusDays(30))
                .validUntil(LocalDateTime.now().minusDays(1))
                .status(CouponStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("CouponAvailabilityValidator")
    class AvailabilityValidatorTest {

        private final CouponAvailabilityValidator validator = new CouponAvailabilityValidator();

        @Test
        @DisplayName("ISSUE, CALCULATE 모두 지원")
        void supports_all() {
            assertThatCode(() -> {
                assert validator.supports(ValidationType.ISSUE);
                assert validator.supports(ValidationType.CALCULATE);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("유효기간 내 - 예외 없음")
        void validate_valid() {
            CouponValidationContext context = CouponValidationContext.forIssue(coupon(10000, 1), 1L);
            assertThatCode(() -> validator.validate(context)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("유효기간 만료 - 예외 발생")
        void validate_expired() {
            CouponValidationContext context = CouponValidationContext.forIssue(expiredCoupon(), 1L);
            assertThatThrownBy(() -> validator.validate(context))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_NOT_AVAILABLE);
        }
    }

    @Nested
    @DisplayName("CouponAlreadyIssuedValidator")
    class AlreadyIssuedValidatorTest {

        @Test
        @DisplayName("ISSUE만 지원")
        void supports_issueOnly() {
            assert alreadyIssuedValidator.supports(ValidationType.ISSUE);
            assert !alreadyIssuedValidator.supports(ValidationType.CALCULATE);
        }

        @Test
        @DisplayName("발급 수량이 한도 미만 - 예외 없음")
        void validate_notIssued() {
            given(memberCouponRepository.countByMemberIdAndCouponId(1L, 1L)).willReturn(0L);
            CouponValidationContext context = CouponValidationContext.forIssue(coupon(10000, 1), 1L);
            assertThatCode(() -> alreadyIssuedValidator.validate(context)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("발급 수량이 한도 이상 - 예외 발생")
        void validate_alreadyIssued() {
            given(memberCouponRepository.countByMemberIdAndCouponId(1L, 1L)).willReturn(1L);
            CouponValidationContext context = CouponValidationContext.forIssue(coupon(10000, 1), 1L);
            assertThatThrownBy(() -> alreadyIssuedValidator.validate(context))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        @Test
        @DisplayName("maxIssuablePerMember가 2이면 2장까지 발급 가능")
        void validate_multipleIssuable() {
            given(memberCouponRepository.countByMemberIdAndCouponId(1L, 1L)).willReturn(1L);
            CouponValidationContext context = CouponValidationContext.forIssue(coupon(10000, 2), 1L);
            assertThatCode(() -> alreadyIssuedValidator.validate(context)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("MinOrderAmountValidator")
    class MinOrderAmountValidatorTest {

        private final MinOrderAmountValidator validator = new MinOrderAmountValidator();

        @Test
        @DisplayName("CALCULATE만 지원")
        void supports_calculateOnly() {
            assert !validator.supports(ValidationType.ISSUE);
            assert validator.supports(ValidationType.CALCULATE);
        }

        @Test
        @DisplayName("최소 주문 금액 이상 - 예외 없음")
        void validate_meetsMinAmount() {
            CouponValidationContext context = CouponValidationContext.forCalculate(coupon(10000, 1), 15000, null);
            assertThatCode(() -> validator.validate(context)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("최소 주문 금액 미충족 - 예외 발생")
        void validate_belowMinAmount() {
            CouponValidationContext context = CouponValidationContext.forCalculate(coupon(10000, 1), 5000, null);
            assertThatThrownBy(() -> validator.validate(context))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
        }
    }

    @Nested
    @DisplayName("CategoryAmountValidator")
    class CategoryAmountValidatorTest {

        private final CategoryAmountValidator validator = new CategoryAmountValidator();

        @Test
        @DisplayName("CALCULATE만 지원")
        void supports_calculateOnly() {
            assert !validator.supports(ValidationType.ISSUE);
            assert validator.supports(ValidationType.CALCULATE);
        }

        @Test
        @DisplayName("applyScope가 null이면 categoryAmount 없어도 예외 없음")
        void validate_noScope_noCategory() {
            CouponValidationContext context = CouponValidationContext.forCalculate(coupon(10000, 1), 15000, null);
            assertThatCode(() -> validator.validate(context)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CATEGORY 스코프에 categoryAmount 있음 - 예외 없음")
        void validate_categoryScope_withAmount() {
            CouponValidationContext context = CouponValidationContext.forCalculate(couponWithCategoryScope(10000), 15000, 12000);
            assertThatCode(() -> validator.validate(context)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CATEGORY 스코프에 categoryAmount 없음 - 예외 발생")
        void validate_categoryScope_missingAmount() {
            CouponValidationContext context = CouponValidationContext.forCalculate(couponWithCategoryScope(10000), 15000, null);
            assertThatThrownBy(() -> validator.validate(context))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCOPE_AMOUNT_REQUIRED);
        }
    }
}
