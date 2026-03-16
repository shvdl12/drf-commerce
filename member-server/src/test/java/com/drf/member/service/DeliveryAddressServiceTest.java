package com.drf.member.service;

import com.drf.common.exception.BusinessException;
import com.drf.common.model.AuthInfo;
import com.drf.member.common.exception.ErrorCode;
import com.drf.member.entitiy.DeliveryAddress;
import com.drf.member.entitiy.Member;
import com.drf.member.model.request.DeliveryAddressCreateRequest;
import com.drf.member.model.request.DeliveryAddressUpdateRequest;
import com.drf.member.model.response.DeliveryAddressResponse;
import com.drf.member.repository.DeliveryAddressRepository;
import com.drf.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DeliveryAddressServiceTest {

    @InjectMocks
    private DeliveryAddressService deliveryAddressService;

    @Mock
    private DeliveryAddressRepository deliveryAddressRepository;

    @Mock
    private MemberRepository memberRepository;

    private Member member;

    private AuthInfo authInfo;

    private DeliveryAddress deliveryAddress;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .id(1L)
                .email("test@test.com")
                .password("encodedPassword")
                .name("홍길동")
                .phone("010-1234-5678")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        authInfo = new AuthInfo(1L);

        deliveryAddress = DeliveryAddress.builder()
                .member(member)
                .name("집")
                .phone("010-1234-5678")
                .address("서울시 강남구")
                .addressDetail("101호")
                .zipCode("12345")
                .isDefault(false)
                .build();
    }

    @Nested
    @DisplayName("배송지 등록")
    class Register {

        @Test
        @DisplayName("첫 번째 배송지면 isDefault가 true로 저장된다")
        void firstAddress_savedAsDefault() {
            // given
            DeliveryAddressCreateRequest request = new DeliveryAddressCreateRequest(
                    "집", "010-1234-5678", "서울시 중랑구", "101호", "12345", false
            );

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(deliveryAddressRepository.existsByMemberId(authInfo.id())).willReturn(false);

            // when
            deliveryAddressService.addDeliverAddress(request, authInfo);

            // then
            then(deliveryAddressRepository).should().save(argThat(DeliveryAddress::isDefault));
        }

        @Test
        @DisplayName("isDefault가 true면 기존 기본 배송지가 해제된다")
        void requestDefault_clearsExistingDefault() {
            // given
            DeliveryAddressCreateRequest request = new DeliveryAddressCreateRequest(
                    "회사", "010-1234-5678", "서울시 광진구", "101호", "54321", true
            );

            DeliveryAddress existingDefault = DeliveryAddress.builder()
                    .member(member)
                    .name("집")
                    .phone("010-1234-5678")
                    .address("서울시 중랑구")
                    .addressDetail("101호")
                    .zipCode("12345")
                    .isDefault(true)
                    .build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(deliveryAddressRepository.findByMemberIdAndIsDefaultTrue(authInfo.id()))
                    .willReturn(Optional.of(existingDefault));

            // when
            deliveryAddressService.addDeliverAddress(request, authInfo);

            // then
            assertThat(existingDefault.isDefault()).isFalse();
        }

        @Test
        @DisplayName("isDefault가 false고 첫 번째 배송지가 아니면 기존 기본 배송지가 유지된다")
        void notDefault_notFirst_existingDefaultUnchanged() {
            // given
            DeliveryAddressCreateRequest request = new DeliveryAddressCreateRequest(
                    "회사", "010-1234-5678", "서울시 광진구", "101호", "54321", false
            );
            AuthInfo authInfo = new AuthInfo(1L);

            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(deliveryAddressRepository.existsByMemberId(authInfo.id())).willReturn(true);

            // when
            deliveryAddressService.addDeliverAddress(request, authInfo);

            // then
            then(deliveryAddressRepository).should(never()).findByMemberIdAndIsDefaultTrue(any());
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외가 발생한다")
        void memberNotFound_throwsException() {
            // given
            DeliveryAddressCreateRequest request = new DeliveryAddressCreateRequest(
                    "집", "010-1234-5678", "서울시 중랑구", "101호", "12345", false
            );

            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryAddressService.addDeliverAddress(request, authInfo))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("배송지 목록 조회")
    class GetDeliverAddress {

        @Test
        @DisplayName("배달 주소 목록을 최신순으로 반환한다")
        void getDeliveryAddresses() {
            // given
            List<DeliveryAddress> addresses = List.of(
                    DeliveryAddress.builder()
                            .member(member)
                            .name("집")
                            .phone("010-1234-5678")
                            .address("서울시 강남구")
                            .addressDetail("101호")
                            .zipCode("12345")
                            .isDefault(true)
                            .build(),
                    DeliveryAddress.builder()
                            .member(member)
                            .name("회사")
                            .phone("010-9876-5432")
                            .address("서울시 서초구")
                            .addressDetail("202호")
                            .zipCode("54321")
                            .isDefault(false)
                            .build()
            );

            given(deliveryAddressRepository.findByMemberIdOrderByIdDesc(authInfo.id())).willReturn(addresses);

            // when
            List<DeliveryAddressResponse> result = deliveryAddressService.getDeliveryAddresses(authInfo);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("집");
            assertThat(result.get(0).isDefault()).isTrue();
            assertThat(result.get(1).name()).isEqualTo("회사");
            assertThat(result.get(1).isDefault()).isFalse();
        }

        @Test
        @DisplayName("배달 주소가 없으면 빈 리스트를 반환한다")
        void getDeliveryAddresses_empty() {
            // given
            given(deliveryAddressRepository.findByMemberIdOrderByIdDesc(authInfo.id())).willReturn(List.of());

            // when
            List<DeliveryAddressResponse> result = deliveryAddressService.getDeliveryAddresses(authInfo);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("배송지 수정")
    class UpdateDeliveryAddress {

        private DeliveryAddress deliveryAddress;

        @BeforeEach
        void setUp() {
            deliveryAddress = DeliveryAddress.builder()
                    .member(member)
                    .name("집")
                    .phone("010-1234-5678")
                    .address("서울시 강남구")
                    .addressDetail("101호")
                    .zipCode("12345")
                    .isDefault(false)
                    .build();
        }

        @Test
        @DisplayName("배송지를 수정한다")
        void updateDeliveryAddress() {
            // given
            DeliveryAddressUpdateRequest request = new DeliveryAddressUpdateRequest(
                    "회사", "010-9876-5432", "서울시 서초구", "202호", "54321", false);

            given(deliveryAddressRepository.findByIdAndMemberId(1L, authInfo.id())).willReturn(Optional.of(deliveryAddress));

            // when
            deliveryAddressService.updateDeliveryAddress(1L, request, authInfo);

            // then
            assertThat(deliveryAddress.getName()).isEqualTo("회사");
            assertThat(deliveryAddress.getPhone()).isEqualTo("010-9876-5432");
        }

        @Test
        @DisplayName("기본 배송지로 설정하면 기존 기본 배송지가 해제된다")
        void updateDeliveryAddress_unmarkPreviousDefault() {
            // given
            DeliveryAddressUpdateRequest request = new DeliveryAddressUpdateRequest(
                    "집", "010-1234-5678", "서울시 강남구", "101호", "12345", true);

            DeliveryAddress previousDefault = DeliveryAddress.builder()
                    .member(member)
                    .name("회사")
                    .phone("010-9876-5432")
                    .address("서울시 서초구")
                    .addressDetail("202호")
                    .zipCode("54321")
                    .isDefault(true)
                    .build();

            given(deliveryAddressRepository.findByIdAndMemberId(1L, authInfo.id())).willReturn(Optional.of(deliveryAddress));
            given(deliveryAddressRepository.findByMemberIdAndIsDefaultTrue(authInfo.id())).willReturn(Optional.of(previousDefault));

            // when
            deliveryAddressService.updateDeliveryAddress(1L, request, authInfo);

            // then
            assertThat(previousDefault.isDefault()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 배송지면 예외가 발생한다")
        void updateDeliveryAddress_notFound() {
            // given
            DeliveryAddressUpdateRequest request = new DeliveryAddressUpdateRequest(
                    "집", "010-1234-5678", "서울시 강남구", "101호", "12345", false);

            given(deliveryAddressRepository.findByIdAndMemberId(1L, authInfo.id())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryAddressService.updateDeliveryAddress(1L, request, authInfo))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DELIVERY_ADDRESS_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("배송지 삭제")
    class DeleteDeliveryAddress {

        @Test
        @DisplayName("배송지를 삭제한다")
        void deleteDeliveryAddress() {
            // given
            given(deliveryAddressRepository.findByIdAndMemberId(1L, authInfo.id()))
                    .willReturn(Optional.of(deliveryAddress));

            // when
            deliveryAddressService.deleteDeliveryAddress(1L, authInfo);

            // then
            then(deliveryAddressRepository).should().delete(deliveryAddress);
        }

        @Test
        @DisplayName("존재하지 않는 배송지면 예외가 발생한다")
        void deleteDeliveryAddress_notFound() {
            // given
            given(deliveryAddressRepository.findByIdAndMemberId(1L, authInfo.id()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> deliveryAddressService.deleteDeliveryAddress(1L, authInfo))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DELIVERY_ADDRESS_NOT_FOUND);
        }

        @Test
        @DisplayName("기본 배송지는 삭제할 수 없다")
        void deleteDeliveryAddress_default() {
            // given
            DeliveryAddress defaultAddress = DeliveryAddress.builder()
                    .member(member)
                    .name("집")
                    .phone("010-1234-5678")
                    .address("서울시 강남구")
                    .addressDetail("101호")
                    .zipCode("12345")
                    .isDefault(true)
                    .build();

            given(deliveryAddressRepository.findByIdAndMemberId(1L, authInfo.id()))
                    .willReturn(Optional.of(defaultAddress));

            // when & then
            assertThatThrownBy(() -> deliveryAddressService.deleteDeliveryAddress(1L, authInfo))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DEFAULT_ADDRESS_CANNOT_BE_DELETED);
        }
    }
}