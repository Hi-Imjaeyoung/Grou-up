package growup.spring.springserver.global.oauth;

import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(OAuth2User oAuth2User) {
        this.attributes = oAuth2User.getAttributes();
    }
    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }
    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getNickname() {
        return (String) attributes.get("name");
    }
    @Override
    public String getProvider() { return "google"; }
}
