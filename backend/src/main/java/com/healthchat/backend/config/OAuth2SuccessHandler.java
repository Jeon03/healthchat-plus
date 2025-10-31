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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

        AtomicBoolean mergedFlag = new AtomicBoolean(false);

        //기존 회원 병합 또는 신규 저장
        User user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    if (existingUser.getProvider() == null || existingUser.getProvider().equalsIgnoreCase("local")) {
                        existingUser.setProvider(provider);
                        userRepository.save(existingUser);
                        mergedFlag.set(true);
                    }
                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .nickname(nickname)
                                .provider(provider)
                                .password("") // OAuth2 계정은 비밀번호 없음
                                .build()
                ));

        //JWT 생성
        String accessToken = jwtTokenProvider.createAccessToken(email);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);

        //Refresh Token 저장
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

        //쿠키 설정
        ResponseCookie accessCookie = buildCookie("access_token", accessToken, 60 * 30);
        ResponseCookie refreshCookie = buildCookie("refresh_token", refreshToken, 60L * 60 * 24 * 14);
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        //세션 완전 제거 (Spring Security 세션 인증 무효화)
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }

        //명시적 302 Redirect
        String redirectUrl = mergedFlag.get()
                ? FRONT + "/?merged=true"
                : FRONT + "/?login=success";

        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", redirectUrl);
    }

    //이메일 추출
    private String extractEmail(DefaultOAuth2User user, String provider) {
        Map<String, Object> attr = user.getAttributes();
        if ("google".equals(provider)) {
            return (String) attr.get("email");
        }
        if ("naver".equals(provider)) {
            Map<String, Object> response = (Map<String, Object>) attr.get("response");
            return (String) response.get("email");
        }
        if ("kakao".equals(provider)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attr.get("kakao_account");
            return (String) kakaoAccount.get("email");
        }
        return "unknown@oauth.com";
    }

    //닉네임 추출
    private String extractNickname(DefaultOAuth2User user, String provider) {
        Map<String, Object> attr = user.getAttributes();
        if ("google".equals(provider)) {
            return (String) attr.get("name");
        }
        if ("naver".equals(provider)) {
            Map<String, Object> response = (Map<String, Object>) attr.get("response");
            return (String) response.get("name");
        }
        if ("kakao".equals(provider)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attr.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            return (String) profile.get("nickname");
        }
        return "사용자";
    }
}
