package growup.spring.springserver.campaign.facade;

import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.campaignoptiondetails.service.CampaignOptionDetailsService;
import growup.spring.springserver.exclusionKeyword.service.ExclusionKeywordService;
import growup.spring.springserver.execution.service.ExecutionService;
import growup.spring.springserver.keyword.service.KeywordService;
import growup.spring.springserver.keywordBid.service.KeywordBidService;
import growup.spring.springserver.margin.service.MarginService;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.service.MarginForCampaignService;
import growup.spring.springserver.memo.service.MemoService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class CampaignDeleteFacade {
    private final CampaignService campaignService;
    private final KeywordService keywordService;
    private final MarginService marginService;
    private final MemoService memoService;
    private final ExecutionService executionService;
    private final CampaignOptionDetailsService campaignOptionDetailsService;
    private final ExclusionKeywordService exclusionKeywordService;
    private final KeywordBidService keywordBidService;
    private final MarginForCampaignService marginForCampaignService;

    @Transactional
    public int deleteCampaign(List<Long> campaignIds){
        return memoService.deleteMemoByCampaignIds(campaignIds)
                + keywordBidService.deleteKeywordBidsByCampaignIds(campaignIds)
                + exclusionKeywordService.deleteExclusionKeywordByCampaignIds(campaignIds)
                + campaignOptionDetailsService.deleteCampaignOptionDetailsByExecutionIds(campaignIds)
                + executionService.deleteExecutionByCampaignIds(campaignIds)
                + marginService.deleteMarginByCampaignIds(campaignIds)
                + keywordService.deleteKeywordByCampaignIds(campaignIds)
                + marginForCampaignService.deleteMFC(campaignIds)
                + campaignService.deleteCampaign(campaignIds);
    }
}
