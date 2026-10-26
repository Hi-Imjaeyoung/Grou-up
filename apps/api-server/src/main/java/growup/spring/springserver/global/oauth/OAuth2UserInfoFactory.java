package growup.spring.springserver.global.oauth;

import lombok.experimental.UtilityClass;
import org.springframework.security.oauth2.core.user.OAuth2User;

@UtilityClass // 🔹 Lombok을 사용하여 인스턴스화 방지
public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, OAuth2User oAuth2User) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> new KakaoOAuth2UserInfo(oAuth2User);
            case "google" -> new GoogleOAuth2UserInfo(oAuth2User);
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + registrationId);
        };
    }
}

// 유틸 클래스에서는 public 생성자가 없어야 합니다.
// 유틸 클래스 만들 때는 private 생성자를 추가해 준다.


