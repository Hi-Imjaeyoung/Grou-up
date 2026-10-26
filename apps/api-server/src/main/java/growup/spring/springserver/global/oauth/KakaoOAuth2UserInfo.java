package growup.spring.springserver.global.oauth;

import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.Map;
import java.util.Optional;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;
    private final Map<String, Object> attributesAccount;
    private final Map<String, Object> attributesProfile;

    public KakaoOAuth2UserInfo(OAuth2User oAuth2User) {
        this.attributes = oAuth2User.getAttributes();
        this.attributesAccount = (Map<String, Object>) attributes.get("kakao_account");
        this.attributesProfile = (Map<String, Object>) attributesAccount.get("profile");
    }

    @Override
    public String getEmail() {
        return safeCastToMap(attributes.get("kakao_account"))
                .map(account -> (String) account.get("email"))
                .orElseThrow(() -> new IllegalArgumentException("Kakao account data is missing"));
    }

    @Override
    public String getNickname() {
        return safeCastToMap(attributes.get("properties"))
                .map(properties -> (String) properties.get("nickname"))
                .orElse("DefaultNickname");
    }
    @Override
    public String getProviderId() { return attributes.get("id").toString(); }

    @Override
    public String getProvider() { return "kakao"; }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> safeCastToMap(Object obj) {
        if (obj instanceof Map) {
            return Optional.of((Map<String, Object>) obj);
        }
        return Optional.empty();
    }
}
