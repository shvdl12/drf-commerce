package com.drf.coupon.service;

import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.Coupon;
import com.drf.coupon.entity.DiscountType;
import com.drf.coupon.model.request.CouponCreateRequest;
import com.drf.coupon.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CouponAdminServiceTest {

    @InjectMocks
    private CouponAdminService couponAdminService;

    @Mock
    private CouponRepository couponRepository;

    @Nested
    @DisplayName("쿠폰 등록")
    class CreateCoupon {

        private CouponCreateRequest request;

        @BeforeEach
        void setUp() {
            request = CouponCreateRequest.builder()
                    .name("신규 가입 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .minOrderAmount(10000)
                    .maxDiscountAmount(null)
                    .applyType(ApplyType.ALL)
                    .applyTargetId(null)
                    .validFrom(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 4, 30, 23, 59))
                    .build();
        }

        @Test
        @DisplayName("등록 성공")
        void createCoupon_success() {
            // given
            Coupon savedCoupon = Coupon.builder()
                    .id(1L)
                    .name(request.name())
                    .discountType(request.discountType())
                    .discountValue(request.discountValue())
                    .totalQuantity(request.totalQuantity())
                    .issuedQuantity(0)
                    .minOrderAmount(request.minOrderAmount())
                    .applyType(request.applyType())
                    .validFrom(request.validFrom())
                    .validUntil(request.validUntil())
                    .build();

            given(couponRepository.save(any(Coupon.class))).willReturn(savedCoupon);

            // when
            Long id = couponAdminService.createCoupon(request);

            // then
            assertThat(id).isEqualTo(1L);
            then(couponRepository).should().save(any(Coupon.class));
        }

        @Test
        @DisplayName("유효기간 종료일이 시작일보다 이전이면 예외 발생")
        void createCoupon_invalidValidDateRange() {
            // given
            request = CouponCreateRequest.builder()
                    .name("신규 가입 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ALL)
                    .validFrom(LocalDateTime.of(2026, 4, 30, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .build();

            // when & then
            assertThatThrownBy(() -> couponAdminService.createCoupon(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_VALID_DATE_RANGE);
        }

        @Test
        @DisplayName("카테고리 쿠폰에 적용 대상이 없으면 예외 발생")
        void createCoupon_categoryWithoutTarget() {
            // given
            request = CouponCreateRequest.builder()
                    .name("카테고리 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.CATEGORY)
                    .applyTargetId(null)
                    .validFrom(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 4, 30, 0, 0))
                    .build();

            // when & then
            assertThatThrownBy(() -> couponAdminService.createCoupon(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CATEGORY_COUPON_REQUIRES_TARGET);
        }

        @Test
        @DisplayName("정률 쿠폰에 최대 할인 금액이 없으면 예외 발생")
        void createCoupon_rateWithoutMaxDiscount() {
            // given
            request = CouponCreateRequest.builder()
                    .name("정률 쿠폰")
                    .discountType(DiscountType.RATE)
                    .discountValue(10)
                    .totalQuantity(100)
                    .minOrderAmount(10000)
                    .maxDiscountAmount(null)
                    .applyType(ApplyType.ALL)
                    .validFrom(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 4, 30, 0, 0))
                    .build();

            // when & then
            assertThatThrownBy(() -> couponAdminService.createCoupon(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.RATE_COUPON_REQUIRES_MAX_DISCOUNT);
        }
    }
}
