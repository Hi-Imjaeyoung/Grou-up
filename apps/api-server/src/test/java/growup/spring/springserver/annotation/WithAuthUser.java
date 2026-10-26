package growup.spring.springserver.annotation;

import growup.spring.springserver.WithMockCustomUserSecurityContextFactory;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithAuthUser {
//    long usersId() default 1L;
    String email() default "test@test.com";
    String password() default "1234abcd!";
    String roles() default "ROLE_SILVER";
}