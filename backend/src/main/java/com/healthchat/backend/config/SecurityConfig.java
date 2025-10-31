package com.healthchat.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // CSRF 비활성화 + CORS 활성화
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())

                // 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/static/**",
                                "/assets/**",
                                "/api/auth/**",   // 로그인, 회원가입, 인증 API
                                "/oauth2/**",     // 소셜 로그인
                                "/login",         // React 라우트
                                "/signup"         // React 라우트
                        ).permitAll()
                        .anyRequest().authenticated()
                )


                .formLogin(form -> form.disable())

                .logout(logout -> logout.disable())

                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(authorization -> authorization.baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(redir -> redir.baseUri("/login/oauth2/code/*"))
                        .successHandler(oAuth2SuccessHandler)
                        .loginPage("/none")
                )

                // JWT 필터 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
