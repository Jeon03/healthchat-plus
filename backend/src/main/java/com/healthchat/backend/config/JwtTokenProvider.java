package com.healthchat.backend.config;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    private static final long ACCESS_EXP = 1000L * 60 * 30; // 30분
    private static final long REFRESH_EXP = 1000L * 60 * 60 * 24 * 14; // 2주

    public String createAccessToken(String email) {
        return buildToken(email, ACCESS_EXP);
    }

    public String createRefreshToken(String email) {
        return buildToken(email, REFRESH_EXP);
    }

    private String buildToken(String email, long validity) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes())
                .compact();
    }

    public String getEmail(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey.getBytes())
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean isExpired(String token) {
        try {
            Date exp = Jwts.parser()
                    .setSigningKey(secretKey.getBytes())
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return exp.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(secretKey.getBytes())
                    .parseClaimsJws(token);
            return true; // ✅ 파싱 성공 → 유효한 토큰
        } catch (ExpiredJwtException e) {
            System.out.println("토큰 만료됨: " + e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("토큰 유효하지 않음: " + e.getMessage());
        }
        return false;
    }
}
