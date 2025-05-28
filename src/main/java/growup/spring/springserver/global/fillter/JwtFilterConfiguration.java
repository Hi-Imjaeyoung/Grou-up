package growup.spring.springserver.global.fillter;

import growup.spring.springserver.global.config.JwtTokenProvider;
import jakarta.servlet.Filter;
import jakarta.servlet.GenericFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Slf4j
public class JwtFilterConfiguration {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtFilterConfiguration(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    @Profile("!bottleNeckTest") // "load-test" 프로파일이 아닐 때
    public GenericFilter jwtAuthenticationProcessingFilter() { // 빈의 이름은 자유롭게 지정 가능
        log.info("JwtFilterConfiguration: JwtAuthFilter 빈 생성!");
        return new JwtAuthFilter(jwtTokenProvider);
    }

    @Bean
    @Profile("bottleNeckTest")  // "load-test" 프로파일일 때
    public GenericFilter jwtAuthenticationProcessingFilterStub() { // 빈의 이름은 다르거나, @Primary 등으로 조절
        log.info("JwtFilterConfiguration: [Stub] JwtAuthFilter 빈 생성!");
        return new JwtAuthFilterStub(jwtTokenProvider);
    }
}
