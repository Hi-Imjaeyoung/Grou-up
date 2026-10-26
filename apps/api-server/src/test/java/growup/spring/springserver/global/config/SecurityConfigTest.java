package growup.spring.springserver.global.config;

import growup.spring.springserver.global.error.UserAccessDeniedHandler;
import growup.spring.springserver.global.error.UserAuthenticationEntryPoint;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@Profile("test")
public class SecurityConfigTest {
    @Bean
    public UserDetailsService userDetailsService() {
        List<UserDetails> userDetails = List.of(
                User.withDefaultPasswordEncoder().username("user").password("user1234").roles("USER").build(),
                User.withDefaultPasswordEncoder().username("admin").password("admin1234").roles("ADMIN").build());
        return new InMemoryUserDetailsManager(userDetails);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/user/**").hasRole("USER")
                        .requestMatchers("/admin/**").hasRole("ADMIN"))
                .formLogin(Customizer.withDefaults())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new AuthenticationEntryPoint() {
                            @Override
                            public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                                response.setCharacterEncoding("UTF-8");
                                response.sendError(401, "인증 오류");
                            }
                        })
                        .accessDeniedHandler(new AccessDeniedHandler() {
                            @Override
                            public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                                response.setCharacterEncoding("UTF-8");
                                response.sendError(403, "인증 오류");
                            }
                        }))
                .build();
    }
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new UserAuthenticationEntryPoint();
    }

    //     403
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new UserAccessDeniedHandler();
    }

    // MockMVC에서 HandlerMappingIntrospector를 사용하기 위함
    // 이게 없으면 requestMatcher 를 정상적으로 하지 못함.
    @Bean(name = "mvcHandlerMappingIntrospector")
    public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
        return new HandlerMappingIntrospector();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

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
