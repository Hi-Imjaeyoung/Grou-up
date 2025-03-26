package growup.spring.springserver.global.common;

import growup.spring.springserver.global.oauth.OAuth2RedirectUrlProvider;
import growup.spring.springserver.global.oauth.OAuth2UserInfo;
import growup.spring.springserver.global.oauth.OAuth2UserInfoFactory;
import growup.spring.springserver.global.config.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

//    @Value("${reDirect.kakao}")
    private String reDirectUrlKakao = "https://www.grouup.co.kr/oauth/kakao/callback?token=%s";

//    @Value("${reDirect.google}")
    private String reDirectUrlGoogle = "https://www.grouup.co.kr/oauth/google/callback?token=%s";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        log.info("onAuthenticationSuccess : 시작");
        // OAuth2AuthenticationToken에서 registrationId 가져오기
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            throw new IllegalArgumentException("Authentication is not an instance of OAuth2AuthenticationToken");
        }
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        log.info("registrationId: {}", registrationId);

        if (registrationId == null) {
            throw new IllegalArgumentException("OAuth provider not found");
        }

        OAuth2User oAuth2User = oauthToken.getPrincipal();
        log.info("oAuth2User :{}", oAuth2User);

        // OAuth2UserInfo 객체 생성
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User);

        String email = userInfo.getEmail();
        String nickname = userInfo.getNickname();
        String role = "ROLE_USER";

        // JWT 토큰 생성
        String memberSpecification = String.format("%s:%s:%s", email, nickname, role);
        String accessToken = jwtTokenProvider.createAccessToken(memberSpecification);
        log.info("accessToken :{}", accessToken);

        response.addHeader("Authorization", "Bearer " + accessToken);

        // 리디렉트 URL 설정
        OAuth2RedirectUrlProvider.setRedirectUrls(reDirectUrlKakao,reDirectUrlGoogle);
        String redirectUrl = OAuth2RedirectUrlProvider.getRedirectUrl(registrationId, accessToken);

        log.info("Redirecting to: {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
