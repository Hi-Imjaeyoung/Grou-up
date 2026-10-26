package growup.spring.springserver.global.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

public interface JwtTokenProvider {
    // 사용자 이메일과 롤을 포함한 액세스 토큰 생성 메서드
    String createAccessToken(String memberSpecification);

    // JWT 토큰을 검증하고 파싱하는 메서드
    Jws<Claims> validateAndParseToken(String token);

    // JWT 토큰을 검증한 후 주제를 반환하는 메서드
    String validateTokenAndGetSubject(String token);
}
