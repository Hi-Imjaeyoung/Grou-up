package growup.spring.springserver.global.fillter;


import growup.spring.springserver.global.config.JwtTokenProvider;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilterStub extends GenericFilter {

    private final transient JwtTokenProvider jwtTokenProvider;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        log.info("[Stub] Jwt Filter Active");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String bearerToken = httpRequest.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("TestToken ")) {
            String token = bearerToken.substring(10);
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
        log.info("next");
        chain.doFilter(request, response);
    }
    private User parseUserSpecification(String token) {
        // subject를 ":"로 구분하여 role 추출
        String userName = token.substring(9);
        StringBuilder userEmailBuilder = new StringBuilder();
        userEmailBuilder.append(userName).append("@test.com");
        // Create User (email, role ) -> @AuthenticationPrincipal
        return new User(userEmailBuilder.toString(), "", List.of(new SimpleGrantedAuthority("ROLE_" + "USER")));
    }
}
