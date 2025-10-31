package com.healthchat.backend.controller;

import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body("로그인 필요");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 정보가 없습니다."));

        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "nickname", user.getNickname(),
                "provider", user.getProvider()
        ));
    }
}
