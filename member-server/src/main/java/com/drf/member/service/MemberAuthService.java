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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

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
}
