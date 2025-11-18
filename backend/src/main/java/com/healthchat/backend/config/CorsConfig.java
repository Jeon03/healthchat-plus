package com.healthchat.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ 쿠키 포함 (JWT HttpOnly 쿠키 전달 가능)
        config.setAllowCredentials(true);

        // ===============================
        // ✅ 배포환경 + 로컬환경 둘 다 허용
        // ===============================
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",      // 로컬 개발환경
                "http://52.78.215.10"         // EC2 배포환경
                // 필요하면 도메인 추가 가능: "https://mydomain.com"
        ));

        // 허용 메서드
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 허용 헤더
        config.setAllowedHeaders(List.of("*"));

        // 클라이언트에서 받을 수 있는 헤더
        config.setExposedHeaders(List.of("Authorization"));

        // Preflight 캐싱 시간
        config.setMaxAge(3600L);

        // 모든 경로에 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
