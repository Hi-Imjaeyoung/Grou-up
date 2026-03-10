package growup.spring.springserver.login.service;

import growup.spring.springserver.global.domain.TypeChange;
import growup.spring.springserver.global.oauth.OAuth2UserInfo;
import growup.spring.springserver.global.oauth.OAuth2UserInfoFactory;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.dto.request.LoginSignUpReqDto;
import growup.spring.springserver.login.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
    private final MemberRepository memberRepository;
    private final TypeChange typeChange;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("Oauth2UserService.loadUser called");

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User);
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        log.info("OAuth provider: {}", userInfo.getProvider());
        log.info("User email: {}", userInfo.getEmail());


        Member member = findOrCreateMember(userInfo);

        if (member.getRecommendationCode() == null || member.getRecommendationCode().isEmpty()) {
            String newCode = MemberService.createRecommendationCode();
            member.updateMember(newCode);
        }

        memberRepository.save(member);

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                oAuth2User.getAttributes(),
                userNameAttributeName
        );
    }
    private Member findOrCreateMember(OAuth2UserInfo userInfo) {
        return memberRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> createNewMember(userInfo));
    }

    // 신규 회원 생성 로직 분리 (private method)
    private Member createNewMember(OAuth2UserInfo userInfo) {
        return typeChange.memberCreateDtoToMember(
                new LoginSignUpReqDto(userInfo.getEmail(), null, userInfo.getNickname()),
                null,
                null // 일단 코드는 null로 생성 (아래에서 통합 처리)
        );
    }
}
