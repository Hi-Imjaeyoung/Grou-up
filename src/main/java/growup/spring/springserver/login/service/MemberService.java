package growup.spring.springserver.login.service;

import growup.spring.springserver.exception.login.MemberInvalidPasswordException;
import growup.spring.springserver.exception.login.MemberKakaoLoginAccountException;
import growup.spring.springserver.exception.login.MemberNotFoundException;
import growup.spring.springserver.global.config.JwtTokenProvider;
import growup.spring.springserver.global.domain.TypeChange;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.dto.request.LoginSignInReqDto;
import growup.spring.springserver.login.dto.request.LoginSignUpReqDto;
import growup.spring.springserver.login.dto.response.LoginDataResDto;
import growup.spring.springserver.login.dto.response.LoginResDto;
import growup.spring.springserver.login.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final TypeChange typeChange;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public void signup(LoginSignUpReqDto loginCreateReqDto) {
        memberRepository.findByEmail(loginCreateReqDto.email())
                .ifPresent(email -> {
                    throw new MemberNotFoundException();
                });
        String encode = passwordEncoder.encode(loginCreateReqDto.password());

        String myCode = createRecommendationCode();

        Member member = typeChange.memberCreateDtoToMember(loginCreateReqDto, encode, myCode);

        memberRepository.save(member);

    }

    static String createRecommendationCode() {

        return UUID.randomUUID().toString().substring(0, 12);
    }

    public LoginResDto login(LoginSignInReqDto loginSignInReqDto) {
        log.info("Check account validity");
        Member findmember = memberRepository.findByEmail(loginSignInReqDto.email())
                .filter(it -> {
                    // 비밀번호가 없는 경우, 카카오 계정으로 가입된 사용자
                    if (it.getPassword() == null || it.getPassword().isEmpty())
                        throw new MemberKakaoLoginAccountException();
                    if (!passwordEncoder.matches(loginSignInReqDto.password(), it.getPassword())) {
                        throw new MemberInvalidPasswordException();
                    }
                    return true;
                })
                .orElseThrow(MemberNotFoundException::new);

        String memberSpecification = String.format("%s:%s:%s", findmember.getEmail(), findmember.getName(), findmember.getRole());

        log.info("member :{}",memberSpecification);
        String accessToken = jwtTokenProvider.createAccessToken(memberSpecification);

        log.info("ACCess token success");
        return typeChange.memberToLoginResDto(findmember, accessToken);
    }

    public LoginDataResDto getMyName(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(
                MemberNotFoundException::new);

        return typeChange.MemberToMyEmailAndRoleDto(member);
    }
    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);
    }
}
