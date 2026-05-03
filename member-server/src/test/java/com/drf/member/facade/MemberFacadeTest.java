package com.drf.member.facade;

import com.drf.common.exception.BusinessException;
import com.drf.member.common.exception.ErrorCode;
import com.drf.member.model.request.MemberSignUpRequest;
import com.drf.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class MemberFacadeTest {

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberFacade memberFacade;

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
    @DisplayName("회원가입 성공 시 id 반환")
    void signUp_success() {
        // given
        given(memberService.saveMember(request)).willReturn(1L);

        // when
        Long id = memberFacade.signUp(request);

        // then
        assertThat(id).isEqualTo(1L);
        then(memberService).should().validateSignUp(request);
        then(memberService).should().saveMember(request);
    }

    @Test
    @DisplayName("검증 실패 시 저장을 호출하지 않는다")
    void signUp_validateFail() {
        // given
        doThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL))
                .when(memberService).validateSignUp(request);

        // when & then
        assertThatThrownBy(() -> memberFacade.signUp(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

        then(memberService).should().validateSignUp(request);
        then(memberService).shouldHaveNoMoreInteractions();
    }
}
