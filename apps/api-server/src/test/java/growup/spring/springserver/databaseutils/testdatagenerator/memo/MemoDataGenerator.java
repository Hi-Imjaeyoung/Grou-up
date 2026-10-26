package growup.spring.springserver.databaseutils.testdatagenerator.memo;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import growup.spring.springserver.memo.domain.Memo;
import growup.spring.springserver.memo.repository.MemoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@ActiveProfiles("bottleNeckTest")
public class MemoDataGenerator {
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private MemoRepository memoRepository;
    @ParameterizedTest
    @CsvSource({
        "user01,"+
        "user500,"+
        "1"
    })
    @DisplayName("모든 캠패인 메모 생성")
    void generateMemo(String startMember,
                      String endMember,
                      int NumberOfMemo){
        List<Member> members = memberRepository.findAll();
        List<Memo> memos = new ArrayList<>();
        for(Member member : members){
            List<Campaign> campaigns = campaignRepository.findAllByMember(member);
            for(Campaign campaign :campaigns){
                Memo memo = Memo.builder()
                        .campaign(campaign)
                        .contents("test memo")
                        .date(LocalDate.of(2025,12,10))
                        .build();
                memos.add(memo);
            }
        }
        memoRepository.saveAll(memos);
    }
}
