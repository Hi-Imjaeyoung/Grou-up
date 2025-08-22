package growup.spring.springserver.databaseutils.testdatagenerator;


import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.global.domain.CoupangExcelData;
import growup.spring.springserver.keyword.domain.Keyword;
import growup.spring.springserver.keyword.repository.KeywordRepository;
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
import java.util.Random;

@SpringBootTest
@ActiveProfiles("bottleNeckTest")
public class CoupangExcelTestDataGenerator {
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private KeywordRepository keywordRepository;

    @ParameterizedTest
    @CsvSource({
            "3, 100, 2025-07-01, 2025-08-01",
    })
    @DisplayName("쿠팡 액셀 데이터 형식 생성")
    void coupangDataGenerator(Long campaignId, int numberOfData, String start, String end){
        while(numberOfData -->0){
            CoupangExcelData coupangExcelData = CoupagRandomData.createRandomData();
            LocalDate date = CoupagRandomData.between(LocalDate.parse(start, DateTimeFormatter.ISO_DATE),LocalDate.parse(end, DateTimeFormatter.ISO_DATE));
            Campaign campaign = campaignRepository.findByCampaignId(campaignId).orElseThrow(CampaignNotFoundException::new);
            Keyword keyword = Keyword.builder()
                    .adCost(coupangExcelData.getAdCost())
                    .cpc(coupangExcelData.getCpc())
                    .campaign(campaign)
                    .cvr(coupangExcelData.getCvr())
                    .adSales(coupangExcelData.getAdSales())
                    .roas(coupangExcelData.getRoas())
                    .impressions(coupangExcelData.getImpressions())
                    .clicks(coupangExcelData.getClicks())
                    .date(date)
                    .keyKeyword(CoupagRandomData.createRandomKeyword())
                    .keySearchType("검색 영역")
                    .clickRate(coupangExcelData.getClickRate())
                    .totalSales(coupangExcelData.getTotalSales())
                    .keyProductSales(CoupagRandomData.createKeyProductSales(coupangExcelData.getTotalSales()))
                    .build();
            keywordRepository.save(keyword);
        }
    }
}
