package com.drf.coupon.service;

import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import com.drf.coupon.entity.*;
import com.drf.coupon.model.request.CouponCreateRequest;
import com.drf.coupon.model.request.CouponUpdateRequest;
import com.drf.coupon.model.response.CouponListResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
                    .minOrderQuantity(0)
                    .maxDiscountAmount(null)
                    .applyType(ApplyType.ORDER)
                    .applyScope(ApplyScope.ALL)
                    .applyTargetId(null)
                    .maxIssuablePerMember(1)
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
                    .applyScope(request.applyScope())
                    .maxIssuablePerMember(1)
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
                    .minOrderQuantity(0)
                    .applyType(ApplyType.ORDER)
                    .applyScope(ApplyScope.ALL)
                    .maxIssuablePerMember(1)
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
        @DisplayName("스코프 대상이 지정되지 않은 쿠폰에 적용 대상 없으면 예외 발생")
        void createCoupon_scopeWithoutTarget() {
            // given
            request = CouponCreateRequest.builder()
                    .name("카테고리 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .minOrderAmount(10000)
                    .minOrderQuantity(0)
                    .applyType(ApplyType.PRODUCT)
                    .applyScope(ApplyScope.CATEGORY)
                    .applyTargetId(null)
                    .maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 4, 30, 0, 0))
                    .build();

            // when & then
            assertThatThrownBy(() -> couponAdminService.createCoupon(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCOPE_TARGET_REQUIRED);
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
                    .minOrderQuantity(0)
                    .maxDiscountAmount(null)
                    .applyType(ApplyType.ORDER)
                    .applyScope(ApplyScope.ALL)
                    .maxIssuablePerMember(1)
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

    @Nested
    @DisplayName("쿠폰 수정")
    class UpdateCoupon {

        private CouponUpdateRequest request;
        private Coupon coupon;

        @BeforeEach
        void setUp() {
            request = CouponUpdateRequest.builder()
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

            coupon = Coupon.builder()
                    .id(1L)
                    .name("원래 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .issuedQuantity(0)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ORDER)
                    .applyScope(ApplyScope.ALL)
                    .maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 4, 30, 23, 59))
                    .status(com.drf.coupon.entity.CouponStatus.ACTIVE)
                    .build();
        }

        @Test
        @DisplayName("수정 성공")
        void updateCoupon_success() {
            // given
            given(couponRepository.findByIdAndStatusNot(eq(1L), eq(CouponStatus.DELETED)))
                    .willReturn(java.util.Optional.of(coupon));

            // when
            couponAdminService.updateCoupon(1L, request);

            // then
            then(couponRepository).should().findByIdAndStatusNot(eq(1L), eq(CouponStatus.DELETED));
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 수정 시 예외 발생")
        void updateCoupon_notFound() {
            // given
            given(couponRepository.findByIdAndStatusNot(eq(999L), eq(CouponStatus.DELETED)))
                    .willReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> couponAdminService.updateCoupon(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_NOT_FOUND);
        }

        @Test
        @DisplayName("유효기간 종료일이 시작일보다 이전이면 예외 발생")
        void updateCoupon_invalidValidDateRange() {
            // given
            request = CouponUpdateRequest.builder()
                    .name("수정된 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(5000)
                    .totalQuantity(200)
                    .minOrderAmount(20000)
                    .minOrderQuantity(0)
                    .applyType(ApplyType.ORDER)
                    .applyScope(ApplyScope.ALL)
                    .maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.of(2026, 5, 31, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 5, 1, 0, 0))
                    .build();

            // when & then
            assertThatThrownBy(() -> couponAdminService.updateCoupon(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_VALID_DATE_RANGE);
        }
    }

    @Nested
    @DisplayName("쿠폰 목록 조회")
    class GetCoupons {

        @Test
        @DisplayName("DELETED 제외한 쿠폰 목록 반환")
        void getCoupons_success() {
            // given
            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .name("신규 가입 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .issuedQuantity(1)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ORDER)
                    .applyScope(ApplyScope.ALL)
                    .maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 4, 30, 23, 59))
                    .status(CouponStatus.ACTIVE)
                    .build();

            given(couponRepository.findByStatusNot(CouponStatus.DELETED)).willReturn(List.of(coupon));

            // when
            List<CouponListResponse> result = couponAdminService.getCoupons();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).couponId()).isEqualTo(1L);
            assertThat(result.get(0).totalQuantity()).isEqualTo(100);
            assertThat(result.get(0).issuedQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("쿠폰이 없으면 빈 목록 반환")
        void getCoupons_empty() {
            // given
            given(couponRepository.findByStatusNot(CouponStatus.DELETED)).willReturn(List.of());

            // when
            List<CouponListResponse> result = couponAdminService.getCoupons();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("쿠폰 삭제")
    class DeleteCoupon {

        private Coupon coupon;

        @BeforeEach
        void setUp() {
            coupon = Coupon.builder()
                    .id(1L)
                    .name("삭제할 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(3000)
                    .totalQuantity(100)
                    .issuedQuantity(0)
                    .minOrderAmount(10000)
                    .applyType(ApplyType.ORDER)
                    .applyScope(ApplyScope.ALL)
                    .maxIssuablePerMember(1)
                    .validFrom(LocalDateTime.of(2026, 4, 1, 0, 0))
                    .validUntil(LocalDateTime.of(2026, 4, 30, 23, 59))
                    .status(CouponStatus.ACTIVE)
                    .build();
        }

        @Test
        @DisplayName("삭제 성공")
        void deleteCoupon_success() {
            // given
            given(couponRepository.findByIdAndStatusNot(eq(1L), eq(CouponStatus.DELETED)))
                    .willReturn(java.util.Optional.of(coupon));

            // when
            couponAdminService.deleteCoupon(1L);

            // then
            then(couponRepository).should().findByIdAndStatusNot(eq(1L), eq(CouponStatus.DELETED));
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 삭제 시 예외 발생")
        void deleteCoupon_notFound() {
            // given
            given(couponRepository.findByIdAndStatusNot(eq(999L), eq(CouponStatus.DELETED)))
                    .willReturn(java.util.Optional.empty());

            // when & then
            assertThatThrownBy(() -> couponAdminService.deleteCoupon(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_NOT_FOUND);
        }
    }
}
