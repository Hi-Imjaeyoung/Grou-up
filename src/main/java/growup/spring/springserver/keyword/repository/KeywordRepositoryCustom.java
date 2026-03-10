package growup.spring.springserver.keyword.repository;

import com.querydsl.core.Tuple;

import java.time.LocalDate;
import java.util.List;

public interface KeywordRepositoryCustom {

    List<Tuple> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmail(LocalDate start, LocalDate end, String email);

    List<Tuple> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(LocalDate start, LocalDate end, String email);

    List<Tuple> getEachCampaignAdCostSumAndAdSalesByEmail(LocalDate start, LocalDate end, String email);

    List<Tuple> getAllTypeOfCampaignAdCostSumAndAdSalesSumByPeriodAndCampaignIds(LocalDate start, LocalDate end, List<Long> campaignIds);
}
