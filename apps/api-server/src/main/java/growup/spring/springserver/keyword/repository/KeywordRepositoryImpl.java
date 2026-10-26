package growup.spring.springserver.keyword.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.util.List;

import static growup.spring.springserver.keyword.domain.QKeyword.keyword;
import static growup.spring.springserver.campaign.domain.QCampaign.campaign;
import static growup.spring.springserver.login.domain.QMember.member;

@RequiredArgsConstructor
public class KeywordRepositoryImpl implements KeywordRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Tuple> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(LocalDate start, LocalDate end, String email){
        var dateBetween = keyword.date.between(
                start,
                end
        );
        return queryFactory.select(keyword.date, campaign.camAdType, keyword.adCost.sum(), keyword.adSales.sum())
                            .from(keyword)
                            .join(keyword.campaign, campaign)
                            .join(campaign.member, member)
                            .where(member.email.eq(email),
                                    dateBetween
                            )
                            .groupBy(keyword.date ,campaign.camAdType)
                            .fetch();
    }

    @Override
    public List<Tuple> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmail(LocalDate start, LocalDate end, String email){
        var dateBetween = keyword.date.between(
                start,
                end
        );
        return queryFactory.select(campaign.camAdType, keyword.adCost.sum(), keyword.adSales.sum())
                .from(keyword)
                .join(keyword.campaign, campaign)
                .join(campaign.member, member)
                .where(member.email.eq(email),
                        dateBetween
                )
                .groupBy(campaign.camAdType)
                .fetch();
    }

    @Override
    public List<Tuple> getEachCampaignAdCostSumAndAdSalesByEmail(LocalDate start, LocalDate end, String email){
        var dateBetween = keyword.date.between(
                start,
                end
        );
        return queryFactory
                .select(campaign.camCampaignName,keyword.adCost.sum(),keyword.adSales.sum())
                .from(keyword)
                .join(keyword.campaign, campaign)
                .join(campaign.member, member)
                .where(member.email.eq(email),
                        dateBetween
                )
                .groupBy(campaign.camCampaignName)
                .fetch();
    }
    public List<Tuple> getAllTypeOfCampaignAdCostSumAndAdSalesSumByPeriodAndCampaignIds(LocalDate start,
                                                                                        LocalDate end,
                                                                                        List<Long> campaignIds){
        var dateBetween = keyword.date.between(start,end);
        return queryFactory
                .select(keyword.date,campaign.camAdType,keyword.adCost.sum(),keyword.adSales.sum())
                .from(keyword)
                .join(keyword.campaign,campaign)
                .where(dateBetween,campaign.campaignId.in(campaignIds))
                .groupBy(campaign.camAdType)
                .fetch();
    }
}
