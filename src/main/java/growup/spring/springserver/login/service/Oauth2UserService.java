package growup.spring.springserver.login.service;

import growup.spring.springserver.global.config.JwtTokenProvider;
import growup.spring.springserver.global.domain.TypeChange;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.dto.request.LoginSignUpReqDto;
import growup.spring.springserver.login.repository.MemberRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class Oauth2UserService extends DefaultOAuth2UserService {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final TypeChange typeChange;

    @Override
    @Transactional // 트랜잭션 관리 추가
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("Oauth2UserService.loadUser called");

        OAuth2User oAuth2User = super.loadUser(userRequest);

        // OAuth 공급자 확인
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth provider: {}", registrationId);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = null;
        String nickname = null;
        String nameAttributeKey;  // 기본 ID 필드 (카카오는 "id" 사용)

        switch (registrationId) {
            case "kakao" -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                email = (String) kakaoAccount.get("email");
                nickname = (String) ((Map<String, Object>) attributes.get("properties")).get("nickname");
                nameAttributeKey = "id";  // 카카오는 "id"를 기본 키로 사용
            }
            case "google" -> {
                email = (String) attributes.get("email");
                nickname = (String) attributes.get("name");
                nameAttributeKey = "sub";  // 구글은 "sub"을 기본 ID로 사용
            }
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        log.info("User email: {}", email);
        log.info("User nickname: {}", nickname);

        // 기존 사용자 체크
        Member member = memberRepository.findByEmail(email).orElse(null);
        String myCode = MemberService.createRecommendationCode();

        // 기존 사용자가 없으면 새로 생성
        if (member == null) {
            member = typeChange.memberCreateDtoToMember(new LoginSignUpReqDto(email, null, nickname), null, myCode);
            memberRepository.save(member);
        } else if (member.getRecommendationCode() == null || member.getRecommendationCode().isEmpty()) {
            member.updateMember(myCode);
        }

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                nameAttributeKey  // 공급자에 따라 ID 필드 변경
        );
    }
}