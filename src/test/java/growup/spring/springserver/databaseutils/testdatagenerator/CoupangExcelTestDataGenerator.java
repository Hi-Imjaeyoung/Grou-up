package growup.spring.springserver.databaseutils.testdatagenerator;


import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.global.domain.CoupangExcelData;
import growup.spring.springserver.keyword.domain.Keyword;
import growup.spring.springserver.keyword.repository.KeywordRepository;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import growup.spring.springserver.margin.domain.Margin;
import net.bytebuddy.asm.Advice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@SpringBootTest
@ActiveProfiles("bottleNeckTest")
public class CoupangExcelTestDataGenerator {
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private KeywordRepository keywordRepository;
    @Autowired
    private MemberRepository memberRepository;

    @ParameterizedTest
    @CsvSource({
            "1000, 2025-07-01, 2025-08-01",
    })
    @DisplayName("쿠팡 액셀 데이터 형식 생성")
    void coupangDataGenerator(int numberOfData, String start, String end){
        RandomDateGenerator randomDateGenerator = new RandomDateGenerator(LocalDate.parse(start,DateTimeFormatter.ISO_DATE),
                LocalDate.parse(end,DateTimeFormatter.ISO_DATE));
        List<Member> members = memberRepository.findAll();
        for(Member member : members) {
            for (Campaign campaign : campaignRepository.findAllByMember(member)) {
                int rowNumber = numberOfData;
                RandomKeywordGenerator randomKeywordGenerator = new RandomKeywordGenerator(campaign, randomDateGenerator);
                while (rowNumber-- > 0) {
                    CoupangExcelData coupangExcelData = CoupagRandomData.createRandomData();
                    Random random = new Random();
                    int randomInt = random.nextInt(1, 11);
                    Keyword keyword;
                    // 비검색 키워드 생성 20%
                    if (randomInt % 5 == 0) {
                        keyword = randomKeywordGenerator.makeNonSearchKeyword(coupangExcelData);
                    } else {
                        keyword = randomKeywordGenerator.makeSearchKeyword(coupangExcelData);
                    }
                    keywordRepository.save(keyword);
                }
            }
        }
    }
}
