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
import com.drf.member.model.request.TokenRefreshRequest;
import com.drf.member.model.response.MemberLoginResponse;
import com.drf.member.model.response.TokenRefreshResponse;
import com.drf.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final AccessTokenBlacklistStore accessTokenBlacklistStore;


    @Transactional
    public MemberLoginResponse memberLogin(MemberLoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        JwtTokenInfo jwtTokenInfo = jwtProvider.generateTokenDetails(member.getId(), Role.USER);
        refreshTokenStore.save(member.getId(), Role.USER, jwtTokenInfo.refreshToken());

        member.updateLastLoginAt();

        return new MemberLoginResponse(
                jwtTokenInfo.accessToken(),
                jwtTokenInfo.refreshToken(),
                "Bearer",
                jwtTokenInfo.expiresIn()
        );
    }

    public void memberLogout(String accessToken, AuthInfo authInfo) {
        // refresh token 삭제
        refreshTokenStore.delete(authInfo.id(), Role.USER);

        //blacklist 등록
        Duration remaining = jwtProvider.getRemainingExpiry(accessToken);
        accessTokenBlacklistStore.save(accessToken, remaining);
    }

    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        Claims claims;
        try {
            claims = jwtProvider.parseToken(request.refreshToken());
            String tokenType = claims.get("type", String.class);

            if (!"refresh".equals(tokenType)) {
                throw new JwtException("Invalid token type: " + tokenType);
            }

        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long memberId = Long.parseLong(claims.getSubject());

        // 저장된 refresh 토큰 조회 및 검증
        String storedRefreshToken = refreshTokenStore.get(memberId, Role.USER);
        if (storedRefreshToken == null || !storedRefreshToken.equals(request.refreshToken())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 토큰 신규 발급
        JwtTokenInfo jwtTokenInfo = jwtProvider.generateTokenDetails(memberId, Role.USER);
        refreshTokenStore.save(memberId, Role.USER, jwtTokenInfo.refreshToken());

        return new TokenRefreshResponse(
                jwtTokenInfo.accessToken(),
                jwtTokenInfo.refreshToken(),
                "Bearer",
                jwtTokenInfo.expiresIn()
        );
    }
}
