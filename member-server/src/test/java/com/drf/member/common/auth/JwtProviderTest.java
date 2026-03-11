package com.drf.member.common.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final Long MEMBER_ID = 1L;
    private static final Role ROLE = Role.USER;
    private static final String SECRET = Base64.getEncoder().encodeToString(
            "test-secret-key-1234567890123456".getBytes()
    );

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, 1800, 604800);
    }

    @Test
    @DisplayName("Access Token과 Refresh Token이 정상 발급된다")
    void generateTokenDetails_success() {
        // when
        JwtTokenInfo tokenInfo = jwtProvider.generateTokenDetails(MEMBER_ID, ROLE);

        // then
        assertThat(tokenInfo.accessToken()).isNotBlank();
        assertThat(tokenInfo.refreshToken()).isNotBlank();
        assertThat(tokenInfo.expiresIn()).isEqualTo(1800);
    }

    @Test
    @DisplayName("Access Token과 Refresh Token은 서로 다르다")
    void generateTokenDetails_accessAndRefreshAreDifferent() {
        // when
        JwtTokenInfo tokenInfo = jwtProvider.generateTokenDetails(MEMBER_ID, ROLE);

        // then
        assertThat(tokenInfo.accessToken()).isNotEqualTo(tokenInfo.refreshToken());
    }

    @Test
    @DisplayName("Access Token의 subject는 memberId다")
    void parseToken_accessToken_subjectIsMemberId() {
        // given
        JwtTokenInfo tokenInfo = jwtProvider.generateTokenDetails(MEMBER_ID, ROLE);

        // when
        Claims claims = jwtProvider.parseToken(tokenInfo.accessToken());

        // then
        assertThat(claims.getSubject()).isEqualTo(String.valueOf(MEMBER_ID));
    }

    @Test
    @DisplayName("Access Token의 role claim이 올바르다")
    void parseToken_accessToken_roleClaimIsCorrect() {
        // given
        JwtTokenInfo tokenInfo = jwtProvider.generateTokenDetails(MEMBER_ID, ROLE);

        // when
        Claims claims = jwtProvider.parseToken(tokenInfo.accessToken());

        // then
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    @DisplayName("Access Token의 type claim은 access다")
    void parseToken_accessToken_typeClaimIsAccess() {
        // given
        JwtTokenInfo tokenInfo = jwtProvider.generateTokenDetails(MEMBER_ID, ROLE);

        // when
        Claims claims = jwtProvider.parseToken(tokenInfo.accessToken());

        // then
        assertThat(claims.get("type", String.class)).isEqualTo("access");
    }

    @Test
    @DisplayName("Refresh Token의 type claim은 refresh다")
    void parseToken_refreshToken_typeClaimIsRefresh() {
        // given
        JwtTokenInfo tokenInfo = jwtProvider.generateTokenDetails(MEMBER_ID, ROLE);

        // when
        Claims claims = jwtProvider.parseToken(tokenInfo.refreshToken());

        // then
        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("Refresh Token에는 role claim이 없다")
    void parseToken_refreshToken_noRoleClaim() {
        // given
        JwtTokenInfo tokenInfo = jwtProvider.generateTokenDetails(MEMBER_ID, ROLE);

        // when
        Claims claims = jwtProvider.parseToken(tokenInfo.refreshToken());

        // then
        assertThat(claims.get("role")).isNull();
    }

    @Test
    @DisplayName("만료된 토큰은 파싱 시 예외가 발생한다")
    void parseToken_expiredToken_throwsException() {
        // given
        JwtProvider expiredJwtProvider = new JwtProvider(SECRET, 0, 0);
        JwtTokenInfo tokenInfo = expiredJwtProvider.generateTokenDetails(MEMBER_ID, ROLE);

        // when & then
        assertThatThrownBy(() -> expiredJwtProvider.parseToken(tokenInfo.accessToken()))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("변조된 토큰은 파싱 시 예외가 발생한다")
    void parseToken_invalidToken_throwsException() {
        // given
        String invalidToken = "invalid.token.value";

        // when & then
        assertThatThrownBy(() -> jwtProvider.parseToken(invalidToken))
                .isInstanceOf(Exception.class);
    }
}