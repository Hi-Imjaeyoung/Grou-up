package growup.spring.springserver.databaseutils.testdatagenerator;


import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.global.domain.CoupangExcelData;
import growup.spring.springserver.keyword.domain.Keyword;
import growup.spring.springserver.keyword.repository.KeywordRepository;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import growup.spring.springserver.margin.TypeChangeMargin;
import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.dto.MarginResponseDto;
import growup.spring.springserver.margin.dto.MarginResultDto;
import growup.spring.springserver.margin.repository.MarginRepository;
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
import java.util.ArrayList;
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
    @Autowired
    private MarginRepository marginRepository;

    @ParameterizedTest
    @CsvSource({
            "1000, 2025-06-01, 2025-07-01",
    })
    @DisplayName("쿠팡 액셀 데이터 형식 생성")
    void coupangDataGenerator(int numberOfData, String start, String end){
        final int BATCH_SIZE = 1000;
        List<Keyword> keywordBatch = new ArrayList<>();
        HashMap<LocalDate, MarginResultDto> map = new HashMap<>();
        RandomDateGenerator randomDateGenerator = new RandomDateGenerator(LocalDate.parse(start,DateTimeFormatter.ISO_DATE),
                LocalDate.parse(end,DateTimeFormatter.ISO_DATE));
        List<Member> members = memberRepository.findAll();
        for(Member member : members) {
            System.out.println(member.getName()+"의 데이터 삽입 시작");
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
                    keywordBatch.add(keyword);
                    if(keywordBatch.size() >BATCH_SIZE){
                        keywordRepository.saveAll(keywordBatch);
                        keywordBatch = new ArrayList<>();
                    }
                    // 마진 데이터 일짜별 합 저장
                    if(map.containsKey(keyword.getDate())){
                        map.get(keyword.getDate()).plusData(coupangExcelData);
                    }else{
                        map.put(keyword.getDate(),MarginResultDto.builder()
                                .marDate(coupangExcelData.getDate())
                                .marClicks(coupangExcelData.getClicks())
                                .marImpressions(coupangExcelData.getImpressions())
                                .marAdCost(coupangExcelData.getAdCost())
                                .marSales(coupangExcelData.getAdSales())
                                .marAdConversionSales(coupangExcelData.getTotalSales())
                                .marAdConversionSalesCount(coupangExcelData.getTotalSales())
                                .build());
                    }
                }
                List<Margin> marginList = new ArrayList<>();
                for(LocalDate date : map.keySet()){
                    MarginResultDto marginResultDto = map.get(date);
                    marginList.add(Margin.builder()
                            .marDate(marginResultDto.getMarDate())
                            .marClicks(marginResultDto.getMarClicks())
                            .marImpressions(marginResultDto.getMarImpressions())
                            .marAdCost(marginResultDto.getMarAdCost())
                            .marSales(marginResultDto.getMarSales())
                            .marAdConversionSales(marginResultDto.getMarAdConversionSales())
                            .marAdConversionSalesCount(marginResultDto.getMarAdConversionSalesCount())
                            .build());
                }
                marginRepository.saveAll(marginList);
                if(!keywordBatch.isEmpty()){
                    keywordRepository.saveAll(keywordBatch);
                }
            }
        }
    }
}
