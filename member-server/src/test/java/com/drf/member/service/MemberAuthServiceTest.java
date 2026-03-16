package com.drf.member.service;

import com.drf.common.exception.BusinessException;
import com.drf.common.model.AuthInfo;
import com.drf.common.model.Role;
import com.drf.member.common.auth.JwtProvider;
import com.drf.member.common.auth.JwtTokenInfo;
import com.drf.member.common.exception.ErrorCode;
import com.drf.member.entitiy.Member;
import com.drf.member.infrastructure.redis.AccessTokenBlacklistStore;
import com.drf.member.infrastructure.redis.RefreshTokenStore;
import com.drf.member.model.request.MemberLoginRequest;
import com.drf.member.model.request.TokenRefreshRequest;
import com.drf.member.model.response.MemberLoginResponse;
import com.drf.member.model.response.TokenRefreshResponse;
import com.drf.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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

    @Nested
    @DisplayName("토큰 갱신")
    class RefreshToken {

        private TokenRefreshRequest request;
        private Claims claims;

        @BeforeEach
        void setUp() {
            request = new TokenRefreshRequest("validRefreshToken");
            claims = mock(Claims.class);
        }

        @Test
        @DisplayName("토큰 갱신 성공 - 새로운 액세스/리프레시 토큰 발급")
        void refreshToken_success() {
            // given
            Long memberId = 1L;
            JwtTokenInfo jwtTokenInfo = new JwtTokenInfo("newAccessToken", "newRefreshToken", 1800);

            given(jwtProvider.parseToken(request.refreshToken())).willReturn(claims);
            given(claims.get("type", String.class)).willReturn("refresh");
            given(claims.getSubject()).willReturn(String.valueOf(memberId));
            given(refreshTokenStore.get(memberId, Role.USER)).willReturn(request.refreshToken());
            given(jwtProvider.generateTokenDetails(memberId, Role.USER)).willReturn(jwtTokenInfo);

            // when
            TokenRefreshResponse response = memberAuthService.refreshToken(request);

            // then
            assertThat(response.accessToken()).isEqualTo("newAccessToken");
            assertThat(response.refreshToken()).isEqualTo("newRefreshToken");
            then(refreshTokenStore).should().save(memberId, Role.USER, "newRefreshToken");
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 만료된 리프레시 토큰")
        void refreshToken_fail_expiredToken() {
            // given
            given(jwtProvider.parseToken(request.refreshToken()))
                    .willThrow(new ExpiredJwtException(null, null, "expired"));

            // when & then
            assertThatThrownBy(() -> memberAuthService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EXPIRED_TOKEN);
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 서명이 유효하지 않은 토큰")
        void refreshToken_fail_invalidSignature() {
            // given
            given(jwtProvider.parseToken(request.refreshToken()))
                    .willThrow(new JwtException("invalid signature"));

            // when & then
            assertThatThrownBy(() -> memberAuthService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 액세스 토큰으로 갱신 시도")
        void refreshToken_fail_wrongTokenType() {
            // given
            given(jwtProvider.parseToken(request.refreshToken())).willReturn(claims);
            given(claims.get("type", String.class)).willReturn("access");

            // when & then
            assertThatThrownBy(() -> memberAuthService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("토큰 갱신 실패 - Redis에 토큰 없음")
        void refreshToken_fail_tokenNotInRedis() {
            // given
            Long memberId = 1L;

            given(jwtProvider.parseToken(request.refreshToken())).willReturn(claims);
            given(claims.get("type", String.class)).willReturn("refresh");
            given(claims.getSubject()).willReturn(String.valueOf(memberId));
            given(refreshTokenStore.get(memberId, Role.USER)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> memberAuthService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("토큰 갱신 실패 - Redis 토큰 불일치 (rotation된 토큰 재사용 시도)")
        void refreshToken_fail_tokenMismatch() {
            // given
            Long memberId = 1L;

            given(jwtProvider.parseToken(request.refreshToken())).willReturn(claims);
            given(claims.get("type", String.class)).willReturn("refresh");
            given(claims.getSubject()).willReturn(String.valueOf(memberId));
            given(refreshTokenStore.get(memberId, Role.USER)).willReturn("alreadyRotatedToken");

            // when & then
            assertThatThrownBy(() -> memberAuthService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }
    }
}
