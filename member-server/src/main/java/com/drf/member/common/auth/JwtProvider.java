package com.drf.member.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final Duration accessTokenExpiry;
    private final Duration refreshTokenExpiry;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiry}") int accessValidity,
            @Value("${jwt.refresh-expiry}") int refreshValidity) {

        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiry = Duration.ofSeconds(accessValidity);
        this.refreshTokenExpiry = Duration.ofSeconds(refreshValidity);
    }

    public JwtTokenInfo generateTokenDetails(Long memberId, Role role) {
        Instant now = Instant.now();

        String accessToken = createAccessToken(now, memberId, role);
        String refreshToken = createRefreshToken(now, memberId);

        int expiresIn = (int) accessTokenExpiry.toSeconds();

        return new JwtTokenInfo(accessToken, refreshToken, expiresIn);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Duration getRemainingExpiry(String accessToken) {
        Date expiration = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(accessToken)
                .getPayload()
                .getExpiration();

        long remaining = expiration.getTime() - System.currentTimeMillis();
        return Duration.ofMillis(Math.max(remaining, 0));
    }

    private String createAccessToken(Instant now, Long memberId, Role role) {
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("role", role.name())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiry)))
                .signWith(secretKey)
                .compact();
    }

    private String createRefreshToken(Instant now, Long memberId) {
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTokenExpiry)))
                .signWith(secretKey)
                .compact();
    }
}
