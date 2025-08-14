package growup.spring.springserver.global.fillter;

import growup.spring.springserver.global.config.JwtTokenProvider;
import growup.spring.springserver.global.config.JwtTokenProviderImpl;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends GenericFilter {
    private final transient JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.secretKey}")
    private String secretKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
//        log.info("JwtAuthFilter activated");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String bearerToken = httpRequest.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            try {
                User user = parseUserSpecification(token);
                AbstractAuthenticationToken authenticated
                        = UsernamePasswordAuthenticationToken.authenticated(user, token, user.getAuthorities());
                authenticated.setDetails(new WebAuthenticationDetails((HttpServletRequest) request));
                SecurityContextHolder.getContext().setAuthentication(authenticated);
            } catch (Exception e) {
                // 401 error
                log.info("Invalid JWT token: {} ", e.getMessage());
            }
        }
//        log.info("next");
        chain.doFilter(request, response);
    }

    private User parseUserSpecification(String token) {
        // JWT 토큰 검증 및 subject 추출
        String subject = Optional.ofNullable(token)
                .filter(t -> !t.isEmpty())
                .map(jwtTokenProvider::validateTokenAndGetSubject)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        // subject를 ":"로 구분하여 role 추출
        String[] split = subject.split(":");
        if (split.length < 2) {
            throw new IllegalArgumentException("토큰의 형식이 올바르지 않습니다.");
        }

        // role claim
        String role = split[2];

        // Create User (email, role ) -> @AuthenticationPrincipal
        return new User(split[0], "", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}