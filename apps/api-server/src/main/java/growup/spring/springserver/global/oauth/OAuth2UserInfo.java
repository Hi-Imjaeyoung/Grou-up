package growup.spring.springserver.global.oauth;

public interface OAuth2UserInfo {
    String getProviderId(); // 소셜 ID (sub, id 등)
    String getProvider();   // google, kakao
    String getEmail();
    String getNickname();
}
