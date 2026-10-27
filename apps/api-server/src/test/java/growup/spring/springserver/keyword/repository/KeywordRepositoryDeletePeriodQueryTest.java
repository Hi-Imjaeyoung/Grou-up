package growup.spring.springserver.keyword.repository;

import com.querydsl.core.Tuple;
import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.global.config.QueryDslConfig;
import growup.spring.springserver.keyword.domain.Keyword;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static growup.spring.springserver.campaign.domain.QCampaign.campaign;
import static growup.spring.springserver.keyword.domain.QKeyword.keyword;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class KeywordRepositoryDeletePeriodQueryTest {

    @Autowired
    private KeywordRepository keywordRepository;
    @Autowired
    private CampaignRepository campaignRepository;

    @Test
    @DisplayName("삭제 기간 집계는 날짜·광고 유형별로 한 행씩 나온다")
    void groupsByDateAndAdType() {
        Campaign saved = campaignRepository.save(Campaign.builder()
                .campaignId(1L)
                .camCampaignName("testCampaign")
                .camAdType("매출최적화")
                .build());
        LocalDate day1 = LocalDate.of(2025, 1, 3);
        LocalDate day2 = LocalDate.of(2025, 1, 4);
        keywordRepository.save(keyword("k1", saved, day1, 10.0, 100.0));
        keywordRepository.save(keyword("k2", saved, day1, 5.0, 50.0));
        keywordRepository.save(keyword("k1", saved, day2, 20.0, 200.0));

        List<Tuple> result = keywordRepository.getAllTypeOfCampaignAdCostSumAndAdSalesSumByPeriodAndCampaignIds(
                day1, day2, List.of(1L));

        assertThat(result)
                .extracting(t -> t.get(keyword.date), t -> t.get(campaign.camAdType),
                        t -> t.get(keyword.adCost.sum()), t -> t.get(keyword.adSales.sum()))
                .containsExactlyInAnyOrder(
                        tuple(day1, "매출최적화", 15.0, 150.0),
                        tuple(day2, "매출최적화", 20.0, 200.0));
    }

    private Keyword keyword(String title, Campaign campaign, LocalDate date, double adCost, double adSales) {
        return Keyword.builder()
                .keyKeyword(title)
                .campaign(campaign)
                .date(date)
                .adCost(adCost)
                .adSales(adSales)
                .build();
    }
}
