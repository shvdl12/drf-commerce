package com.drf.member.model.response;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        int expiresIn
) {
}
