package com.drf.member.model.response;

public record MemberLoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        int expiresIn
) {
}
