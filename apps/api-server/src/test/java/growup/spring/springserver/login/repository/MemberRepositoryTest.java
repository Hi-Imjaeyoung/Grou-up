package growup.spring.springserver.login.repository;
import growup.spring.springserver.global.support.Role;
import growup.spring.springserver.login.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;


import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;


    @Test
    void success_createDB() {
        assertThat(memberRepository).isNotNull();
    }

    @Test
    void success_saveData() {
        // given
        Member newMember = newMember();

        // when
        memberRepository.save(newMember);

        // then
        assertThat(newMember.getEmail()).isEqualTo("test@naver.com");
        assertThat(newMember.getRole()).isEqualTo(Role.GOLD);
    }
    @Test
    void success_selectData(){
        // given
        Member newMember = newMember();

        // when
        memberRepository.save(newMember);
        final Member byEmail = memberRepository.findByEmail("test@naver.com").orElseThrow(NullPointerException::new);

        //given
        assertThat(byEmail).isNotNull();
        assertThat(byEmail.getEmail()).isEqualTo("test@naver.com");
        
    }
    public Member newMember(){
        return Member.builder().email("test@naver.com").role(Role.GOLD).build();
    }

}
