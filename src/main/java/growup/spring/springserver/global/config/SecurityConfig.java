package growup.spring.springserver.global.config;


import growup.spring.springserver.global.common.OAuth2AuthenticationSuccessHandler;
import growup.spring.springserver.global.error.UserAccessDeniedHandler;
import growup.spring.springserver.global.error.UserAuthenticationEntryPoint;
import growup.spring.springserver.global.fillter.JwtAuthFilter;
import growup.spring.springserver.login.service.Oauth2UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
@RequiredArgsConstructor
//@EnableGlobalMethodSecurity(prePostEnabled = true) // PreAuthorize
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final Oauth2UserService oauth2UserService;
    private final OAuth2AuthenticationSuccessHandler successHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        log.info("SecurityConfig activated");
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)  // CSRF 비활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // CORS 설정

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // 세션 정책 설정

                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)  // JWT 필터 추가

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(LoginApiUrl).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(successHandler) // 커스텀 성공 핸들러
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oauth2UserService) // 사용자 정보 서비스 등록
                        )
                );
        return httpSecurity.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> httpMethodList = List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        );
        List<String> ipList = List.of("http://localhost:8000", "https://www.grouup.co.kr","http://localhost:3000");

        config.setAllowCredentials(true);
        config.setAllowedMethods(httpMethodList);
        config.setAllowedOrigins(ipList);
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    private static final String[] LoginApiUrl = {
            "/api/members/signup",
            "/api/members/test1",
            "/api/members/login",
            "/api/oauth/kakao/callback/**",
            "/v3/api-docs/**",          // OpenAPI 문서 JSON
            "/v3/api-docs.yaml",        // OpenAPI 문서 YAML 형식
            "/swagger-ui/**",           // Swagger UI 웹 페이지 리소스
            "/swagger-ui.html",         // Swagger UI 메인 페이지
            "/swagger-resources/**",    // Swagger 리소스
            "/webjars/**" ,              // Swagger UI에서 사용하는 웹 자원들
            "/favicon.ico"
    };

//    401
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new UserAuthenticationEntryPoint();
    }

//     403
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new UserAccessDeniedHandler();
    }
//   role
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
        ROLE_ADMIN > ROLE_GOLD
        ROLE_GOLD > ROLE_SILVER
        ROLE_SILVER > ROLE_BRONZE
        ROLE_BRONZE > ROLE_USER
        """);
    }
}
