package growup.spring.springserver.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Slf4j
public class JwTokenProviderConfiguration {

    @Bean
    @Profile("!bottleNeckTest")
    public JwtTokenProvider jwtTokenProvider(){
        log.info("JwTokenProviderConfiguration : jwtProvider 생성");
        return new JwtTokenProviderImpl();
    }

    @Bean
    @Profile("bottleNeckTest")
    public JwtTokenProvider jwtTokenProviderStub(){
        log.info("JwTokenProviderConfiguration : [Stub] jwtProvider 생성");
        return new JwtTokenProviderStubImpl();
    }
}
