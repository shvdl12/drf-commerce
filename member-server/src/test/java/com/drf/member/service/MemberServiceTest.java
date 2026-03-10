package com.drf.member.service;

import com.drf.member.common.exception.BusinessException;
import com.drf.member.common.exception.ErrorCode;
import com.drf.member.entitiy.Member;
import com.drf.member.event.signup.MemberSignUpEvent;
import com.drf.member.model.request.MemberSignUpRequest;
import com.drf.member.repository.MemberRepository;
import com.drf.member.repository.WithdrawnMemberHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
        verify(eventPublisher).publishEvent(any(MemberSignUpEvent.class));
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
