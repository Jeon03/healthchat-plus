package com.healthchat.backend.service;

import com.healthchat.backend.entity.User;
import com.healthchat.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /** ✅ 로그인된 사용자 정보 조회 */
    public ResponseEntity<?> getMyInfo(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("로그인 정보가 없습니다.");
        }

        String email = extractEmail(authentication);

        if (email == null || email.isBlank()) {
            log.warn("⚠️ 이메일을 추출할 수 없습니다. authentication={}", authentication);
            return ResponseEntity.status(400).body("이메일을 가져올 수 없습니다.");
        }

        log.info("✅ 최종 추출된 이메일 = {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 정보가 없습니다."));

        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "nickname", user.getNickname(),
                "provider", user.getProvider()
        ));
    }

    /** ✅ 소셜 / 로컬 로그인 구분 후 이메일 추출 */
    private String extractEmail(Authentication authentication) {
        try {
            // ✅ OAuth2 로그인 (소셜)
            if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
                String provider = oauthToken.getAuthorizedClientRegistrationId();

                log.info("🔍 OAuth2 attributes ({}): {}", provider, attributes);

                switch (provider.toLowerCase()) {
                    case "google" -> {
                        return (String) attributes.get("email");
                    }
                    case "naver" -> {
                        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                        return (String) response.get("email");
                    }
                    case "kakao" -> {
                        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                        return (String) kakaoAccount.get("email");
                    }
                    default -> {
                        log.warn("⚠️ 알 수 없는 provider: {}", provider);
                        return null;
                    }
                }
            }

            // ✅ 로컬 로그인 (JWT 기반)
            return authentication.getName();

        } catch (Exception e) {
            log.error("이메일 추출 중 오류: {}", e.getMessage());
            return null;
        }
    }
}
