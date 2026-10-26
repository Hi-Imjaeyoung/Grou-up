package growup.spring.springserver.databaseutils.testdatagenerator;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.global.domain.CoupangExcelData;
import growup.spring.springserver.keyword.domain.Keyword;

import java.util.HashMap;

public class RandomKeywordGenerator {
    Campaign campaign;
    RandomDateGenerator randomDateGenerator;
    RandomKeywordGenerator(Campaign campaign,RandomDateGenerator randomDateGenerator){
        this.campaign = campaign;
        this.randomDateGenerator = randomDateGenerator;
    }
    public  Keyword makeNonSearchKeyword(CoupangExcelData coupangExcelData) {
        return Keyword.builder()
                .adCost(coupangExcelData.getAdCost())
                .cpc(coupangExcelData.getCpc())
                .campaign(campaign)
                .cvr(coupangExcelData.getCvr())
                .adSales(coupangExcelData.getAdSales())
                .roas(coupangExcelData.getRoas())
                .impressions(coupangExcelData.getImpressions())
                .clicks(coupangExcelData.getClicks())
                .date(randomDateGenerator.getLocalDate())
                .keyKeyword("-")
                .keySearchType("비 검색 영역")
                .clickRate(coupangExcelData.getClickRate())
                .totalSales(coupangExcelData.getTotalSales())
                .keyProductSales(new HashMap<>())
                .build();
    };
    public  Keyword makeSearchKeyword(CoupangExcelData coupangExcelData){
        return Keyword.builder()
                .adCost(coupangExcelData.getAdCost())
                .cpc(coupangExcelData.getCpc())
                .campaign(campaign)
                .cvr(coupangExcelData.getCvr())
                .adSales(coupangExcelData.getAdSales())
                .roas(coupangExcelData.getRoas())
                .impressions(coupangExcelData.getImpressions())
                .clicks(coupangExcelData.getClicks())
                .date(randomDateGenerator.getLocalDate())
                .keyKeyword(CoupagRandomData.createRandomKeyword())
                .keySearchType("검색 영역")
                .clickRate(coupangExcelData.getClickRate())
                .totalSales(coupangExcelData.getTotalSales())
                .keyProductSales(CoupagRandomData.createKeyProductSales(coupangExcelData.getTotalSales()))
                .build();
    };
}
