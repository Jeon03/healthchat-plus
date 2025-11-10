package com.healthchat.backend.config;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractTokenFromCookies(request);

            // 토큰이 존재하고, 유효할 때만 인증 시도
            if (token != null && jwtTokenProvider.validateToken(token)) {
                String email = jwtTokenProvider.getEmail(token);
                var userDetails = userDetailsService.loadUserByUsername(email);

                // ✅ CustomUserDetails를 principal로 넣어야 컨트롤러에서 @AuthenticationPrincipal 사용 가능
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, // ✅ principal: CustomUserDetails
                        null,
                        userDetails.getAuthorities()
                );


//                var authToken = new UsernamePasswordAuthenticationToken(
//                        email, // 👈 핵심: 이제 authentication.getName() == email
//                        null,
//                        userDetails.getAuthorities()
//                );


                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();

        } catch (Exception e) {
            System.out.println("JWT 인증 실패: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**쿠키에서 access_token 추출 */
    private String extractTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
