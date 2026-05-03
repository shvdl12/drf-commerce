package com.drf.member.service;

import com.drf.common.exception.BusinessException;
import com.drf.common.model.AuthInfo;
import com.drf.member.common.exception.ErrorCode;
import com.drf.member.entitiy.Member;
import com.drf.member.entitiy.MemberStatus;
import com.drf.member.entitiy.WithdrawnMemberHistory;
import com.drf.member.event.internal.MemberSignUpEvent;
import com.drf.member.model.request.MemberSignUpRequest;
import com.drf.member.model.request.PasswordUpdateRequest;
import com.drf.member.model.request.ProfileUpdateRequest;
import com.drf.member.model.response.MemberProfileResponse;
import com.drf.member.repository.MemberRepository;
import com.drf.member.repository.WithdrawnMemberHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private WithdrawnMemberHistoryRepository withdrawnMemberHistoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Nested
    @DisplayName("회원가입")
    class signUp {
        private MemberSignUpRequest request;

        @BeforeEach
        void setUp() {
            request = MemberSignUpRequest.builder()
                    .email("test@test.com")
                    .password("password123!")
                    .name("홍길동")
                    .phone("010-1234-5678")
                    .birthDate(LocalDate.of(2026, 1, 1))
                    .build();
        }

        @Test
        @DisplayName("회원가입 성공")
        void signUp_success() {
            // given
            given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
            given(withdrawnMemberHistoryRepository.existsByEmailAndRejoinAllowedAtAfter(
                    request.getEmail(), LocalDate.now())).willReturn(false);
            given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");
            Member savedMember = mock(Member.class);
            given(savedMember.getId()).willReturn(1L);
            given(memberRepository.save(any(Member.class))).willReturn(savedMember);

            // when
            Long id = memberService.signUp(request);

            // then
            assertThat(id).isEqualTo(1);
            then(eventPublisher).should().publishEvent(any(MemberSignUpEvent.class));
        }

        @Test
        @DisplayName("이메일 중복 시 예외 발생")
        void signUp_duplicateEmail() {
            // given
            given(memberRepository.existsByEmail(request.getEmail())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        }

        @Test
        @DisplayName("이메일 중복 시 예외 발생 (유니크 에러)")
        void signUp_duplicateEmail_UniqueError() {
            // given
            given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
            given(withdrawnMemberHistoryRepository.existsByEmailAndRejoinAllowedAtAfter(
                    request.getEmail(), LocalDate.now())).willReturn(false);
            given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");
            given(memberRepository.save(any(Member.class)))
                    .willThrow(new DataIntegrityViolationException("이메일 중복"));

            // when & then
            assertThatThrownBy(() -> memberService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        }

        @Test
        @DisplayName("재가입 제한 기간 내 가입 시 예외 발생")
        void signUp_rejoinNotAllowed() {
            // given
            given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
            given(withdrawnMemberHistoryRepository.existsByEmailAndRejoinAllowedAtAfter(
                    request.getEmail(), LocalDate.now())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.REJOIN_NOT_ALLOWED);
        }
    }

    @Nested
    @DisplayName("회원 프로필 조회")
    class GetMemberProfile {

        private Member member;
        private AuthInfo authInfo;

        @BeforeEach
        void setUp() {
            member = Member.create(
                    "test@test.com",
                    "encodedPassword",
                    "홍길동",
                    "010-1234-5678",
                    LocalDate.of(1995, 1, 1)
            );
            authInfo = new AuthInfo(1L);
        }

        @Test
        @DisplayName("프로필 조회 성공")
        void getMemberProfile_success() {
            // given
            given(memberRepository.findById(authInfo.id())).willReturn(Optional.of(member));

            // when
            MemberProfileResponse response = memberService.getMemberProfile(authInfo);

            // then
            assertThat(response.email()).isEqualTo("test@test.com");
            assertThat(response.name()).isEqualTo("홍길동");
            assertThat(response.phone()).isEqualTo("010-1234-5678");
            assertThat(response.birthDate()).isEqualTo(LocalDate.of(1995, 1, 1));
            assertThat(response.status()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외 발생")
        void getMemberProfile_fail_memberNotFound() {
            // given
            given(memberRepository.findById(authInfo.id())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.getMemberProfile(authInfo))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("프로필 수정")
    class UpdateProfile {

        private Member member;
        private AuthInfo authInfo;

        @BeforeEach
        void setUp() {
            member = Member.create(
                    "test@test.com",
                    "encodedPassword",
                    "홍길동",
                    "010-1234-5678",
                    LocalDate.of(1995, 1, 1)
            );
            authInfo = new AuthInfo(1L);
        }

        @Test
        @DisplayName("이름과 전화번호 둘 다 수정 성공")
        void updateProfile_success_both() {
            // given
            ProfileUpdateRequest request = new ProfileUpdateRequest("홍길똥", "010-9999-8888");
            given(memberRepository.findById(authInfo.id())).willReturn(Optional.of(member));

            // when
            memberService.updateProfile(request, authInfo);

            // then
            assertThat(member.getName()).isEqualTo("홍길똥");
            assertThat(member.getPhone()).isEqualTo("010-9999-8888");
        }

        @Test
        @DisplayName("이름만 수정 성공 - 전화번호 기존 값 유지")
        void updateProfile_success_nameOnly() {
            // given
            ProfileUpdateRequest request = new ProfileUpdateRequest("홍길똥", null);
            given(memberRepository.findById(authInfo.id())).willReturn(Optional.of(member));

            // when
            memberService.updateProfile(request, authInfo);

            // then
            assertThat(member.getName()).isEqualTo("홍길똥");
            assertThat(member.getPhone()).isEqualTo("010-1234-5678");
        }

        @Test
        @DisplayName("전화번호만 수정 성공 - 이름 기존 값 유지")
        void updateProfile_success_phoneOnly() {
            // given
            ProfileUpdateRequest request = new ProfileUpdateRequest(null, "010-9999-8888");
            given(memberRepository.findById(authInfo.id())).willReturn(Optional.of(member));

            // when
            memberService.updateProfile(request, authInfo);

            // then
            assertThat(member.getName()).isEqualTo("홍길동");
            assertThat(member.getPhone()).isEqualTo("010-9999-8888");
        }

        @Test
        @DisplayName("이름, 전화번호 둘 다 null이면 기존 값 유지")
        void updateProfile_success_nothingChanged() {
            // given
            ProfileUpdateRequest request = new ProfileUpdateRequest(null, null);
            given(memberRepository.findById(authInfo.id())).willReturn(Optional.of(member));

            // when
            memberService.updateProfile(request, authInfo);

            // then
            assertThat(member.getName()).isEqualTo("홍길동");
            assertThat(member.getPhone()).isEqualTo("010-1234-5678");
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외 발생")
        void updateProfile_fail_memberNotFound() {
            // given
            ProfileUpdateRequest request = new ProfileUpdateRequest("김민승", null);
            given(memberRepository.findById(authInfo.id())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.updateProfile(request, authInfo))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class UpdatePassword {

        private Member member;
        private AuthInfo authInfo;

        @BeforeEach
        void setUp() {
            member = Member.create(
                    "test@test.com",
                    "encodedPassword",
                    "홍길동",
                    "010-1234-5678",
                    LocalDate.of(1995, 1, 1)
            );
            authInfo = new AuthInfo(1L);
        }

        @Test
        @DisplayName("업데이트 성공")
        void updatePassword_success() {
            // given
            PasswordUpdateRequest request = new PasswordUpdateRequest("currentPassword", "NewPassword1!");
            given(memberRepository.findById(authInfo.id())).willReturn(Optional.of(member));
            given(passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())).willReturn(true);
            given(passwordEncoder.encode(request.getNewPassword())).willReturn("newEncodedPassword");

            // when
            memberService.updatePassword(request, authInfo);

            // then
            assertThat(member.getPassword()).isEqualTo("newEncodedPassword");
        }


        @Test
        @DisplayName("현재 비밀번호 불일치 시 예외 발생")
        void updatePassword_fail_invalidCurrentPassword() {
            // given
            PasswordUpdateRequest request = new PasswordUpdateRequest("wrongPassword", "NewPassword1!");
            given(memberRepository.findById(authInfo.id())).willReturn(Optional.of(member));
            given(passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> memberService.updatePassword(request, authInfo))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_PASSWORD);
        }

        @Test
        @DisplayName("현재 비밀번호와 신규 비밀번호가 같으면 예외 발생")
        void updatePassword_fail_sameAsCurrentPassword() {
            // given
            PasswordUpdateRequest request = new PasswordUpdateRequest("Password1!", "Password1!");
            given(memberRepository.findById(authInfo.id())).willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> memberService.updatePassword(request, authInfo))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NEW_PASSWORD_MUST_BE_DIFFERENT);
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외 발생")
        void updatePassword_fail_memberNotFound() {
            // given
            PasswordUpdateRequest request = new PasswordUpdateRequest("currentPassword", "NewPassword1!");
            given(memberRepository.findById(authInfo.id())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.updatePassword(request, authInfo))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class WithdrawMember {

        private Member member;
        private AuthInfo authInfo;

        @BeforeEach
        void setUp() {
            member = Member.create(
                    "test@test.com",
                    "encodedPassword",
                    "홍길동",
                    "010-1234-5678",
                    LocalDate.of(1995, 1, 1)
            );
            authInfo = new AuthInfo(1L);
        }

        @Test
        @DisplayName("탈퇴 성공")
        void withdrawMember_success() {
            // given
            given(memberRepository.findById(authInfo.id())).willReturn(Optional.of(member));

            // when
            memberService.withdrawMember(authInfo);

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
            then(withdrawnMemberHistoryRepository).should().save(any(WithdrawnMemberHistory.class));
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외 발생")
        void withdrawMember_fail_memberNotFound() {
            // given
            given(memberRepository.findById(authInfo.id())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.withdrawMember(authInfo))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 탈퇴한 회원이면 예외 발생")
        void withdrawMember_fail_alreadyWithdrawn() {
            // given
            member.withdraw();
            given(memberRepository.findById(authInfo.id())).willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> memberService.withdrawMember(authInfo))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CANNOT_WITHDRAW);
        }
    }
}
