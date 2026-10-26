package growup.spring.springserver;

import growup.spring.springserver.annotation.WithAuthUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.ArrayList;
import java.util.List;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithAuthUser> {
    @Override
    public SecurityContext createSecurityContext(WithAuthUser annotation) {
        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(annotation.roles()));
        User user = new User(annotation.email(),"", authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        securityContext.setAuthentication(authentication);
        return securityContext;
    }
}
