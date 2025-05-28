package growup.spring.springserver.databaseutils.testdatagenerator.campaign;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.databaseutils.testdatagenerator.DataFormat;
import growup.spring.springserver.global.support.Role;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.test.context.ActiveProfiles;

import javax.persistence.criteria.CriteriaBuilder;

@SpringBootTest
@ActiveProfiles("bottleNeckTest")
public class campaignDataGenerator {
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private MemberRepository memberRepository;
    /*
        설정한 범위 내 user 번호 (startNumber) ~ (EndNumber) 멤버에
        캠패인 N개 추가
        캠패인 ID는 맨 끝 캠패인의 Id를 입력해줘야합니다.
    */
    @ParameterizedTest
    @CsvSource({
            "01, 30, 30",
    })
    @DisplayName("CampaignGenerator")
    void campaignGenerator(String startUserNumber, String endUserNumber ,String numberOfCampaign){
        int startNum = Integer.parseInt(startUserNumber);
        int endNum = Integer.parseInt(endUserNumber);
        Long campaignId = 1L;
        while(startNum <= endNum){
            Member member;
            String counter = String.valueOf(startNum);
            if(startNum<10) {
                counter = "0" + startNum;
            }
            System.out.println(counter);
            String email = DataFormat.USERNAME.getValue() +counter+ DataFormat.EMAIL_FORMAT.getValue();
            try {
                 member = memberRepository.findByEmail(email).orElseThrow(
                         ChangeSetPersister.NotFoundException::new
                 );
            }catch (ChangeSetPersister.NotFoundException e){
                System.out.println("Member를 못 찾았습니다.");
                continue;
            }
            for(int i = 1; i<=Integer.parseInt(numberOfCampaign); i++){
                campaignRepository.save(Campaign.builder()
                        .campaignId(campaignId)
                        .camCampaignName("testCam"+counter)
                        .member(member)
                        .camAdType("매출최적화")
                        .build()
                );
                campaignId++;
            }
            startNum++;
        }
    }

}
