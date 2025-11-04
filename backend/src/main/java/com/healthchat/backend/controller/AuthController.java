package com.healthchat.backend.controller;

import com.healthchat.backend.dto.auth.LoginRequest;
import com.healthchat.backend.dto.auth.SignupRequest;
import com.healthchat.backend.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // -----------------------------------
    // 🧩 이메일 인증 기반 회원가입
    // -----------------------------------

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> body) {
        return authService.sendCode(body.get("email"));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody SignupRequest req) {
        return authService.verifyCode(req);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        return authService.signup(req);
    }

    // -----------------------------------
    // 🧩 로그인 / 토큰 / 로그아웃
    // -----------------------------------

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletResponse response,
                                     @CookieValue(value = "refresh_token", required = false) String refreshToken) {
        return authService.refresh(refreshToken, response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response,
                                    @CookieValue(value = "refresh_token", required = false) String refreshToken) {
        return authService.logout(refreshToken, response);
    }

    // -----------------------------------
    // 🧩 계정 병합 (로컬 ↔ 소셜)
    // -----------------------------------

    @PostMapping("/merge-account")
    public ResponseEntity<?> mergeAccount(@RequestBody Map<String, String> body) {
        return authService.mergeAccount(body.get("email"), body.get("provider"));
    }
}
