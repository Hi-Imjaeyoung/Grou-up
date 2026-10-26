package growup.spring.springserver.global.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import org.springframework.beans.factory.annotation.Value;

/*
    Test User Key Format : "TestToken : GUTokenUser%d%d"
    About memberSpecification format :
    Email format "User%d%d@test.com"
    Name format "USER%d%d"
    Set every User role "USER"
 */
public class JwtTokenProviderStubImpl implements JwtTokenProvider{

    @Override
    public String createAccessToken(String memberSpecification) {
        StringBuilder testTokenBuilder =  new StringBuilder();
        testTokenBuilder.append("GUToken");
        String[] memberSpecifications = memberSpecification.split(":");
        testTokenBuilder.append(memberSpecifications[1]);
        return testTokenBuilder.toString();
    }

    @Override
    public Jws<Claims> validateAndParseToken(String token) {
        return new Jws<Claims>() {
            @Override
            public String getSignature() {
                return "test";
            }

            @Override
            public JwsHeader getHeader() {
                return null;
            }

            @Override
            public Claims getBody() {
                return null;
            }
        };
    }

    @Override
    public String validateTokenAndGetSubject(String token) {
        return validateAndParseToken(token).getBody().getSubject();
    }
}
