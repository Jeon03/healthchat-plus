package com.healthchat.backend.controller;

import com.healthchat.backend.dto.ProfileRequest;
import com.healthchat.backend.dto.ProfileResponse;
import com.healthchat.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** ✅ 현재 로그인된 사용자 정보 조회 */
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(Authentication authentication) {
        return userService.getMyInfo(authentication);
    }

    /** ✅ 프로필 저장 (대시보드에서 설정 페이지로 전송) */
    @PostMapping("/profile")
    public ResponseEntity<Void> saveProfile(
            Authentication authentication,
            @RequestBody ProfileRequest request
    ) {
        // ✅ 이메일 기반으로 처리
        String email = authentication.getName();
        userService.saveProfileByEmail(email, request);
        return ResponseEntity.ok().build();
    }

    /** ✅ 프로필 조회 (대시보드에서 불러오기) */
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        // ✅ 이메일 기반으로 처리
        String email = authentication.getName();
        ProfileResponse response = userService.getProfileByEmail(email);
        return ResponseEntity.ok(response);
    }
}
