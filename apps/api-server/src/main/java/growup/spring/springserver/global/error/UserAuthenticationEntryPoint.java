package growup.spring.springserver.global.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

// 403 에러
@Slf4j
public class UserAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        ErrorResponseDto.writeToResponse(response, HttpStatus.UNAUTHORIZED, authException.getMessage());
        logExceptionMessage(authException);
    }

    private void logExceptionMessage(AuthenticationException authenticationException) {
        log.warn("Unauthorized error occurred: {}", authenticationException.getMessage());
    }
}
