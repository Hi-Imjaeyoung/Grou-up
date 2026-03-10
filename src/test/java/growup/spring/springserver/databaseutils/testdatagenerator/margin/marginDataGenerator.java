package growup.spring.springserver.databaseutils.testdatagenerator.margin;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.databaseutils.testdatagenerator.DataFormat;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.repository.MarginRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;
import java.util.List;

@SpringBootTest
@ActiveProfiles("bottleNeckTest")
public class marginDataGenerator {
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private MarginRepository marginRepository;
    /*
        사용자 번호 시작부터 끝까지
        N개의 캠패인에 대하여
        시작 날짜 부터 31일 마진 데이터를 생성
     */
    @ParameterizedTest
    @CsvSource(value = {
        "01, 30, 10, 2025-05-01"
    })
    @DisplayName("MarginGenerator")
    void marginGenerator(String startUserNumber, String endUserNumber, int campaignNumber,
                         String startDate){
        int startUserNum = Integer.parseInt(startUserNumber);
        int endUserNum = Integer.parseInt(endUserNumber);

        while(startUserNum<=endUserNum){
            String userName = DataFormat.convertNumberToUserName(startUserNumber);
            String userEmail = DataFormat.convertNumberToUserEmail(startUserNumber);
            Member member ;
            try{
                member = memberRepository.findByEmail(userEmail).orElseThrow(
                         ChangeSetPersister.NotFoundException::new
                );
            }catch (ChangeSetPersister.NotFoundException e){
                System.out.println(userEmail + "을 찾지 못함");
                startUserNum++;
                continue;
            }
            List<Campaign> campaigns = campaignRepository.findAllByMember(member);
            for(int i=0;i<campaignNumber;i++){
                if(campaigns.size() <= i){
                    System.out.println("캠패인 수가 입력된 캠패인의 수보다 적습니다.");
                    break;
                }
                Campaign campaign = campaigns.get(i);
                LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
                int counter = 1;
                while(counter ++ <= 30){
                    marginRepository.save(Margin.builder()
                                    .campaign(campaign)
                                    .marDate(start)
                                    .marTargetEfficiency(10.0)
                                    .marAdMargin(1000L)
                                    .marTargetEfficiency(20.0)
                                    .marActualSales(10L)
                                    .build());
                    start = start.plusDays(1);
                }
            }
            startUserNum++;
        }
    }
}
