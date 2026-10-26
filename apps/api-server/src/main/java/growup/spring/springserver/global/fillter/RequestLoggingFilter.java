package growup.spring.springserver.global.fillter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Set<String> EXCLUDED_URLS = Set.of(
            "/api/members/test1",
            "/actuator/prometheus"
            // 앞으로 추가하고 싶은 경로가 생기면 여기에 추가
    );
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
//        System.out.println(uri);
        if (!EXCLUDED_URLS.contains(uri)) {
            String method = request.getMethod();
            String queryString = request.getQueryString();
            if (queryString != null) {
                uri += "?" + queryString;
            }
            log.info("Request Received: {} {}", method, uri);
        }
        // 로그를 찍든 안 찍든, 다음 필터로의 전달은 항상 실행되어야 함
        filterChain.doFilter(request, response);
    }
}