package growup.spring.springserver.global.config;

import growup.spring.springserver.global.fillter.JwtFilterConfiguration;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@ContextConfiguration(classes = {SecurityConfigTest.class, JwtTokenProvider.class,JwTokenProviderConfiguration.class})
class JwtTokenProviderTest {
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.secretKey}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long expiration;

    @BeforeEach
    void setUp() {
        // secretKey와 expiration 값이 제대로 주입되었는지 확인
        assertThat(secretKey).isNotNull();
        assertThat(expiration).isNotNull();
    }

    @Test
    @DisplayName("토큰 생성 테스트")
    void testCreateAccessToken() {
        // given
        String memberSpecification = "test@test.com:TestUser:USER";

        // when
        String token = jwtTokenProvider.createAccessToken(memberSpecification);

        // then
        assertThat(token).isNotNull();
        assertThat(token).contains(".");
    }

    @Test
    @DisplayName("토큰 검증 테스트")
    void testValidateTokenAndGetSubject() {
        // given
        String memberSpecification = "test@test.com:TestUser:USER";
        String token = jwtTokenProvider.createAccessToken(memberSpecification);

        // when
        String subject = jwtTokenProvider.validateTokenAndGetSubject(token);

        //then
        assertThat(subject).isEqualTo(memberSpecification);
    }

    @Test
    @DisplayName("만료된 토큰 검증 테스트")
    void testExpiredToken() {
        // Given
        String expiredToken = Jwts.builder()
                .signWith(getSigningKey())
                .setSubject("expired@test.com:ExpiredUser:USER")
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // 1시간 전
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60)) // 이미 만료됨
                .compact();

        // Then
        assertThrows(ExpiredJwtException.class, () -> {
            // When
            jwtTokenProvider.validateTokenAndGetSubject(expiredToken);
        });
    }
    @Test
    @DisplayName("유효하지 않은 토큰 검증 테스트 - 서명 오류")
    void testInvalidSignatureToken() {
        // Given
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QHRlc3QuY29tOlRlc3RVc2VyOlVTRVIiLCJpYXQiOjE2ODAwMDAwMDB9.WRONG_SIGNATURE";

        // Then
        assertThrows(SignatureException.class, () -> {
            // When
            jwtTokenProvider.validateTokenAndGetSubject(invalidToken);
        });
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}