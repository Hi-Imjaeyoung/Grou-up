package growup.spring.springserver.global.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
public class JwtTokenProviderImpl implements JwtTokenProvider {

    @Value("${jwt.secretKey}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long expiration;

    // 서명에 사용할 SecretKey 생성 메서드
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    // 사용자 이메일과 롤을 포함한 액세스 토큰 생성 메서드
    @Override
    public String createAccessToken(String memberSpecification) {
        return Jwts.builder()
                .signWith(this.getSigningKey()) // 서명 키
                .setSubject(memberSpecification) // 토큰의 주제
                .setIssuedAt(Timestamp.valueOf(LocalDateTime.now()))
                .setExpiration(Date.from(Instant.now().plus(360, ChronoUnit.MINUTES)))
                .compact();
    }

    // JWT 토큰을 검증하고 파싱하는 메서드
    @Override
    public Jws<Claims> validateAndParseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(this.getSigningKey()) // 서명 키 설정 (토큰 검증용)
                .build()
                .parseClaimsJws(token); // 토큰을 파싱하고 서명 검증
    }

    // JWT 토큰을 검증한 후 주제를 반환하는 메서드
    @Override
    public String validateTokenAndGetSubject(String token) {
        return validateAndParseToken(token).getBody().getSubject();
    }
}

