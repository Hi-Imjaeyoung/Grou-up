package growup.spring.springserver.databaseutils.testdatagenerator.member;

import growup.spring.springserver.databaseutils.testdatagenerator.DataFormat;
import growup.spring.springserver.global.support.Role;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.xml.crypto.Data;

@SpringBootTest
@ActiveProfiles("bottleNeckTest")
//@Disabled
public class memberDataGenerator {
    @Autowired
    private MemberRepository memberRepository;
    final Role defaultRole = Role.USER;


    @ParameterizedTest
    @ValueSource(ints = {500})
    @DisplayName("MemberGenerator")
    void memberGenerator(int numberOfMember){
        for(int i=1;i<=numberOfMember;i++){
            String counter = String.valueOf(i);
            if(i < 10) counter = "0" + i;
            memberRepository.save(
                    Member.builder()
                    .name(DataFormat.USERNAME.getValue() + counter)
                    .email(DataFormat.USERNAME.getValue()+ counter +DataFormat.EMAIL_FORMAT.getValue())
                    .role(defaultRole)
                    .build()
            );
        }
    }

}
