package com.healthchat.backend.config;

import com.healthchat.backend.entity.RefreshToken;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.RefreshTokenRepository;
import com.healthchat.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.frontend-url}")
    private String FRONT;

    @Value("${app.cookie.domain:}")
    private String COOKIE_DOMAIN;

    @Value("${app.cookie.secure:false}")
    private boolean COOKIE_SECURE;

    @Value("${app.cookie.same-site:Lax}")
    private String COOKIE_SAME_SITE;

    // ✅ 쿠키 빌더
    private ResponseCookie buildCookie(String name, String value, long maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(COOKIE_SECURE)
                .sameSite(COOKIE_SAME_SITE)
                .path("/")
                .maxAge(maxAge);

        if (COOKIE_DOMAIN != null && !COOKIE_DOMAIN.isBlank()) {
            builder.domain(COOKIE_DOMAIN);
        }
        return builder.build();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) oauthToken.getPrincipal();

        String provider = oauthToken.getAuthorizedClientRegistrationId(); // google, naver, kakao
        String email = extractEmail(oAuth2User, provider);
        String nickname = extractNickname(oAuth2User, provider);

        // ✅ 기존 사용자 확인
        User existingUser = userRepository.findByEmail(email).orElse(null);

        if (existingUser != null) {
            // ✅ 이미 병합된 동일 provider → 바로 로그인 처리
            if (existingUser.getProvider() != null && existingUser.getProvider().equalsIgnoreCase(provider)) {
                issueJwtTokens(response, email);

                // 세션 초기화 및 인증정보 정리
                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }
                SecurityContextHolder.clearContext();

                response.sendRedirect(FRONT + "/?login=success");
                return;
            }

            // ✅ provider가 다를 때만 병합 안내로 리디렉션
            String redirectUrl = String.format("%s/?mergeCandidate=%s&provider=%s", FRONT, email, provider);
            response.sendRedirect(redirectUrl);
            return;
        }

        // ✅ 신규 사용자 → 등록 후 로그인 처리
        User newUser = User.builder()
                .email(email)
                .nickname(nickname)
                .provider(provider)
                .password("") // 소셜 로그인은 비밀번호 없음
                .build();
        userRepository.save(newUser);

        issueJwtTokens(response, email);

        // ✅ 세션 및 인증정보 완전 초기화
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        SecurityContextHolder.clearContext();

        response.sendRedirect(FRONT + "/?login=success");
    }

    // ✅ JWT 토큰 발급 및 쿠키 설정
    private void issueJwtTokens(HttpServletResponse response, String email) {
        String accessToken = jwtTokenProvider.createAccessToken(email);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);

        refreshTokenRepository.findByEmail(email)
                .ifPresentOrElse(
                        token -> {
                            token.setToken(refreshToken);
                            refreshTokenRepository.save(token);
                        },
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .email(email)
                                        .token(refreshToken)
                                        .build()
                        )
                );

        ResponseCookie accessCookie = buildCookie("access_token", accessToken, 60 * 30);
        ResponseCookie refreshCookie = buildCookie("refresh_token", refreshToken, 60L * 60 * 24 * 14);
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    // ✅ 이메일 추출
    private String extractEmail(DefaultOAuth2User user, String provider) {
        Map<String, Object> attr = user.getAttributes();
        return switch (provider) {
            case "google" -> (String) attr.get("email");
            case "naver" -> ((Map<String, Object>) attr.get("response")).get("email").toString();
            case "kakao" -> ((Map<String, Object>) ((Map<String, Object>) attr.get("kakao_account"))).get("email").toString();
            default -> "unknown@oauth.com";
        };
    }

    // ✅ 닉네임 추출
    private String extractNickname(DefaultOAuth2User user, String provider) {
        Map<String, Object> attr = user.getAttributes();
        return switch (provider) {
            case "google" -> (String) attr.get("name");
            case "naver" -> ((Map<String, Object>) attr.get("response")).get("name").toString();
            case "kakao" -> ((Map<String, Object>) ((Map<String, Object>) attr.get("kakao_account")).get("profile")).get("nickname").toString();
            default -> "사용자";
        };
    }
}
