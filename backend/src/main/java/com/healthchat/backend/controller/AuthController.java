package com.healthchat.backend.controller;

import com.healthchat.backend.config.JwtTokenProvider;
import com.healthchat.backend.dto.auth.LoginRequest;
import com.healthchat.backend.dto.auth.SignupRequest;
import com.healthchat.backend.entity.RefreshToken;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.RefreshTokenRepository;
import com.healthchat.backend.repository.UserRepository;
import com.healthchat.backend.service.MailService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MailService mailService;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.cookie.domain:}")
    private String COOKIE_DOMAIN;

    @Value("${app.cookie.secure:false}")
    private boolean COOKIE_SECURE;

    @Value("${app.cookie.same-site:Lax}")
    private String COOKIE_SAME_SITE;

    /** 공통 쿠키 빌더 */
    private ResponseCookie buildCookie(String name, String value, long maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(COOKIE_SECURE)
                .sameSite(COOKIE_SAME_SITE)
                .path("/")
                .maxAge(maxAge);

        // localhost에서는 domain 생략
        if (COOKIE_DOMAIN != null && !COOKIE_DOMAIN.isBlank() && maxAge > 0) {
            builder.domain(COOKIE_DOMAIN);
        }
        return builder.build();
    }

    // -----------------------------------
    // 🧩 이메일 인증 기반 회원가입
    // -----------------------------------

    /** 1단계: 인증 코드 발송 */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("❌ 이미 등록된 이메일입니다.");
        }
        mailService.sendVerificationCode(email);
        return ResponseEntity.ok("✅ 인증 코드가 이메일로 전송되었습니다.");
    }

    /** ✅ 2단계: 인증 코드 검증 + 회원가입 처리 */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Validated @RequestBody SignupRequest req) {
        String key = "verify:" + req.getEmail();
        String savedCode = redisTemplate.opsForValue().get(key);

        if (savedCode == null || !savedCode.equals(req.getCode())) {
            return ResponseEntity.badRequest().body("❌ 인증 코드가 유효하지 않습니다.");
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body("❌ 이미 등록된 이메일입니다.");
        }

        // ✅ 새 사용자 저장
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setGender(req.getGender());
        user.setBirthDate(LocalDate.parse(req.getBirthDate()));

        userRepository.save(user);
        redisTemplate.delete(key);

        return ResponseEntity.ok("🎉 회원가입이 완료되었습니다!");
    }

    // -----------------------------------
    // 🧩 로그인 / 토큰 / 로그아웃
    // -----------------------------------

    /** ✅ 로그인 */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated @RequestBody LoginRequest req, HttpServletResponse response) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("❌ 비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        refreshTokenRepository.findByEmail(user.getEmail())
                .ifPresentOrElse(
                        token -> {
                            token.setToken(refreshToken);
                            refreshTokenRepository.save(token);
                        },
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .email(user.getEmail())
                                        .token(refreshToken)
                                        .build()
                        )
                );

        ResponseCookie accessCookie = buildCookie("access_token", accessToken, 60 * 30);
        ResponseCookie refreshCookie = buildCookie("refresh_token", refreshToken, 60L * 60 * 24 * 14);

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "nickname", user.getNickname()
        ));
    }

    /** ✅ Access Token 재발급 */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || jwtTokenProvider.isExpired(refreshToken)) {
            return ResponseEntity.status(401).body("Refresh Token 만료. 다시 로그인 필요.");
        }

        String email = jwtTokenProvider.getEmail(refreshToken);
        RefreshToken saved = refreshTokenRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Refresh Token 없음"));

        if (!saved.getToken().equals(refreshToken)) {
            return ResponseEntity.status(401).body("Refresh Token 불일치");
        }

        String newAccess = jwtTokenProvider.createAccessToken(email);
        ResponseCookie newAccessCookie = buildCookie("access_token", newAccess, 60 * 30);

        response.addHeader(HttpHeaders.SET_COOKIE, newAccessCookie.toString());
        return ResponseEntity.ok("✅ 새 Access Token 발급 완료");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken != null) {
            String email = jwtTokenProvider.getEmail(refreshToken);
            refreshTokenRepository.findByEmail(email)
                    .ifPresent(refreshTokenRepository::delete);
        }

        SecurityContextHolder.clearContext();

        ResponseCookie clearAccess = buildCookie("access_token", "", 0);
        ResponseCookie clearRefresh = buildCookie("refresh_token", "", 0);

        response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());

        return ResponseEntity.ok("✅ 로그아웃 완료");
    }
}
