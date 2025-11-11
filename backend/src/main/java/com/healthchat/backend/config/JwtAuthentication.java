package com.healthchat.backend.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 *  Jwt 인증 객체
 * Spring Security의 Authentication 구현체 중 하나.
 * AccessToken에서 추출한 이메일을 인증 주체로 사용.
 */
public class JwtAuthentication extends AbstractAuthenticationToken {

    private final String email; // JWT에서 추출된 사용자 이메일

    public JwtAuthentication(String email, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.email = email;
        setAuthenticated(true); // 이미 검증된 JWT이므로 인증 완료 상태로 설정
    }

    public JwtAuthentication(String email) {
        this(email, Collections.emptyList());
    }

    @Override
    public Object getCredentials() {
        return null; // JWT 인증은 별도 비밀번호가 없음
    }

    @Override
    public Object getPrincipal() {
        return email; // principal로 사용자 이메일을 반환
    }
}
