package com.drf.member.common.auth;

public record JwtTokenInfo(
        String accessToken,
        String refreshToken,
        int expiresIn
) {
}
