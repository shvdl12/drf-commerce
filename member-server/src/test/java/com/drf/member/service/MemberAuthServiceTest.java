package com.drf.member.service;

import com.drf.member.common.auth.JwtProvider;
import com.drf.member.common.auth.JwtTokenInfo;
import com.drf.member.common.auth.Role;
import com.drf.member.common.exception.BusinessException;
import com.drf.member.common.exception.ErrorCode;
import com.drf.member.common.model.AuthInfo;
import com.drf.member.entitiy.Member;
import com.drf.member.infrastructure.redis.AccessTokenBlacklistStore;
import com.drf.member.infrastructure.redis.RefreshTokenStore;
import com.drf.member.model.request.MemberLoginRequest;
import com.drf.member.model.response.MemberLoginResponse;
import com.drf.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MemberAuthServiceTest {

    @InjectMocks
    private MemberAuthService memberAuthService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private AccessTokenBlacklistStore accessTokenBlacklistStore;


    @Nested
    @DisplayName("로그인")
    class Login {
        private MemberLoginRequest request;

        @BeforeEach
        void setUp() {
            request = new MemberLoginRequest("test@test.com", "password123!");
        }

        @Test
        @DisplayName("로그인 성공 시 토큰이 반환된다")
        void memberLogin_success() {
            // given
            Member member = mock(Member.class);
            given(member.getId()).willReturn(1L);
            given(member.getPassword()).willReturn("encodedPassword");
            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));
            given(passwordEncoder.matches(request.getPassword(), "encodedPassword")).willReturn(true);
            given(jwtProvider.generateTokenDetails(1L, Role.USER))
                    .willReturn(new JwtTokenInfo("accessToken", "refreshToken", 1800));

            // when
            MemberLoginResponse response = memberAuthService.memberLogin(request);

            // then
            assertThat(response.accessToken()).isEqualTo("accessToken");
            assertThat(response.refreshToken()).isEqualTo("refreshToken");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(1800);
        }

        @Test
        @DisplayName("로그인 성공 시 Refresh Token이 Redis에 저장된다")
        void memberLogin_refreshTokenSavedToRedis() {
            // given
            Member member = mock(Member.class);
            given(member.getId()).willReturn(1L);
            given(member.getPassword()).willReturn("encodedPassword");
            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));
            given(passwordEncoder.matches(request.getPassword(), "encodedPassword")).willReturn(true);
            given(jwtProvider.generateTokenDetails(1L, Role.USER))
                    .willReturn(new JwtTokenInfo("accessToken", "refreshToken", 1800));

            // when
            memberAuthService.memberLogin(request);

            // then
            then(refreshTokenStore).should().save(1L, Role.USER, "refreshToken");
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시 예외가 발생한다")
        void memberLogin_emailNotFound_throwsException() {
            // given
            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberAuthService.memberLogin(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("비밀번호 불일치 시 예외가 발생한다")
        void memberLogin_passwordMismatch_throwsException() {
            // given
            Member member = mock(Member.class);
            given(member.getPassword()).willReturn("encodedPassword");
            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));
            given(passwordEncoder.matches(request.getPassword(), "encodedPassword")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> memberAuthService.memberLogin(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {
        private AuthInfo authInfo;

        @BeforeEach
        void setUp() {
            authInfo = new AuthInfo(1L);
        }

        @Test
        @DisplayName("로그아웃 성공 - refresh token 삭제 및 blacklist 등록")
        void logout_success() {
            // given
            String accessToken = "validAccessToken";
            Duration remaining = Duration.ofMinutes(25);

            given(jwtProvider.getRemainingExpiry(accessToken)).willReturn(remaining);

            // when
            memberAuthService.memberLogout(accessToken, authInfo);

            // then
            then(refreshTokenStore).should().delete(authInfo.id(), Role.USER);
            then(accessTokenBlacklistStore).should().save(accessToken, remaining);
        }
    }
}
