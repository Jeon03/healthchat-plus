package com.healthchat.backend.service;

import com.healthchat.backend.config.JwtTokenProvider;
import com.healthchat.backend.dto.auth.LoginRequest;
import com.healthchat.backend.dto.auth.SignupRequest;
import com.healthchat.backend.entity.RefreshToken;
import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.RefreshTokenRepository;
import com.healthchat.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

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

    /** ✅ 쿠키 공통 빌더 */
    private ResponseCookie buildCookie(String name, String value, long maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(COOKIE_SECURE)
                .sameSite(COOKIE_SAME_SITE)
                .path("/")
                .maxAge(maxAge);

        if (COOKIE_DOMAIN != null && !COOKIE_DOMAIN.isBlank() && maxAge > 0) {
            builder.domain(COOKIE_DOMAIN);
        }
        return builder.build();
    }

    // -----------------------------------
    // 🧩 이메일 인증 기반 회원가입
    // -----------------------------------

    public ResponseEntity<?> sendCode(String email) {
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("이미 등록된 이메일입니다.");
        }
        mailService.sendVerificationCode(email);
        return ResponseEntity.ok("✅ 인증 코드가 이메일로 전송되었습니다.");
    }

    public ResponseEntity<?> verifyCode(SignupRequest req) {
        String key = "verify:" + req.getEmail();
        String savedCode = redisTemplate.opsForValue().get(key);

        if (savedCode == null || !savedCode.equals(req.getCode())) {
            return ResponseEntity.badRequest().body("인증 코드가 유효하지 않습니다.");
        }

        return ResponseEntity.ok("✅ 인증이 완료되었습니다.");
    }

    public ResponseEntity<?> signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body("이미 등록된 이메일입니다.");
        }

        String key = "verify:" + req.getEmail();
        String savedCode = redisTemplate.opsForValue().get(key);
        if (savedCode == null) {
            return ResponseEntity.badRequest().body("이메일 인증이 필요합니다.");
        }

        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setGender(req.getGender());
        user.setBirthDate(LocalDate.parse(req.getBirthDate()));
        user.setProvider("local");

        userRepository.save(user);
        redisTemplate.delete(key);

        return ResponseEntity.ok("회원가입이 완료되었습니다!");
    }

    // -----------------------------------
    // 🧩 로그인 / 토큰 / 로그아웃
    // -----------------------------------

    public ResponseEntity<?> login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("등록되지 않은 이메일입니다."));

        // ✅ 소셜 로그인 계정 안내
        if (user.getProvider() != null && !"local".equalsIgnoreCase(user.getProvider())) {
            String msg = "⚠️ 이 이메일은 <b>" + user.getProvider().toUpperCase()
                    + "</b> 계정과 이미 연동되어 있습니다.<br/>"
                    + "👉 <b>" + user.getProvider().toUpperCase() + " 소셜 로그인</b>을 이용해주세요.";
            return ResponseEntity.badRequest().body(Map.of("message", msg));
        }

        // ✅ 비밀번호 불일치
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "비밀번호가 일치하지 않습니다."));
        }

        // ✅ JWT 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        // ✅ Refresh Token 저장/갱신
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

        // ✅ 쿠키 발급
        ResponseCookie accessCookie = buildCookie("access_token", accessToken, 60 * 30);
        ResponseCookie refreshCookie = buildCookie("refresh_token", refreshToken, 60L * 60 * 24 * 14);

        // ✅ JSON 응답 반환
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of(
                        "message", "✅ 로그인 성공",
                        "nickname", user.getNickname()
                ));
    }


    public ResponseEntity<?> refresh(String refreshToken, HttpServletResponse response) {
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

    public ResponseEntity<?> logout(String refreshToken, HttpServletResponse response) {
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

        return ResponseEntity.ok("로그아웃 완료");
    }

    // -----------------------------------
    // 🧩 계정 병합
    // -----------------------------------

    public ResponseEntity<?> mergeAccount(String email, String provider) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일이 존재하지 않습니다."));

        if (user.getProvider() != null && !"local".equalsIgnoreCase(user.getProvider())) {
            return ResponseEntity.badRequest().body("이미 " + user.getProvider() + " 계정과 연동되어 있습니다.");
        }

        user.setProvider(provider);
        userRepository.save(user);

        return ResponseEntity.ok("계정 병합 완료 — 이제 " + provider.toUpperCase() + " 로그인을 사용할 수 있습니다.");
    }
}
