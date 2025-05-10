package growup.spring.springserver.login.service;

import growup.spring.springserver.exception.login.MemberNotFoundException;
import growup.spring.springserver.global.domain.TypeChange;
import growup.spring.springserver.global.support.Role;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.dto.response.LoginDataResDto;
import growup.spring.springserver.login.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @InjectMocks
    private MemberService memberService;
    @Mock
    private TypeChange typeChange;
    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("getMyEmailAndRole(): ErrorCase1. 해당 멤버를 찾지 못할 때")
    void test1(){
        // given
        doReturn(Optional.empty()).when(memberRepository).findByEmail(any(String.class));
        // when
        MemberNotFoundException ex = assertThrows(
                MemberNotFoundException.class,
                () -> memberService.getMyName("test@test.com")
        );
        //then
        assertThat(ex.getMessage()).isEqualTo("존재하지 않는 회원입니다.");
    }

    @Test
    @DisplayName("getMyEmailAndRole(): Success")
    void test2() {
        // given
        Member mockMember = getMember(); // 동일 객체 사용

        doReturn(Optional.of(mockMember)).when(memberRepository).findByEmail(any(String.class));
        doReturn(new LoginDataResDto("test@test.com"))
                .when(typeChange).MemberToMyEmailAndRoleDto(mockMember); // 동등성 비교

        // when
        LoginDataResDto result = memberService.getMyName("test@test.com");

        // then
        assertThat(result.name()).isNotNull();


        // verify
        verify(memberRepository, times(1)).findByEmail("test@test.com");
        verify(typeChange, times(1)).MemberToMyEmailAndRoleDto(mockMember);
    }
    public Member getMember(){
        return Member.builder()
                .email("test@test.com")
                .role(Role.GOLD)
                .build();
    }
}