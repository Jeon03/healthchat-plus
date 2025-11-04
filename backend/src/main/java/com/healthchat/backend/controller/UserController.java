package com.healthchat.backend.controller;

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
}
