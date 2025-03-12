package growup.spring.springserver.global.oauth;

import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class OAuth2RedirectUrlProvider {
    private static final Map<String, String> REDIRECT_URLS = new HashMap<>();

    static {
        REDIRECT_URLS.put("kakao", "https://www.grouup.co.kr/oauth/kakao/callback?token=%s");
        REDIRECT_URLS.put("google", "https://www.grouup.co.kr/oauth/google/callback?token=%s");
    }

    public static String getRedirectUrl(String registrationId, String accessToken) {
        if (!REDIRECT_URLS.containsKey(registrationId)) {
            throw new IllegalArgumentException("지원하지 않는 OAuth 공급자: " + registrationId);
        }
        return String.format(REDIRECT_URLS.get(registrationId), accessToken);
    }
}
