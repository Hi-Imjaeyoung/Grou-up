package growup.spring.springserver.campaign.facade;

import com.querydsl.core.Tuple;
import growup.spring.springserver.campaign.dto.CampaignDeleteDto;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.campaignoptiondetails.service.CampaignOptionDetailsService;
import growup.spring.springserver.exclusionKeyword.service.ExclusionKeywordService;
import growup.spring.springserver.execution.dto.ExecutionMarginResDto;
import growup.spring.springserver.execution.service.ExecutionService;
import growup.spring.springserver.global.cache.AllCampaignTypeData;
import growup.spring.springserver.global.cache.LazySegmentTreeService;
import growup.spring.springserver.global.listener.TreeBuildEvent;
import growup.spring.springserver.global.listener.TreeUpdateEvent;
import growup.spring.springserver.keyword.service.KeywordService;
import growup.spring.springserver.keywordBid.service.KeywordBidService;
import growup.spring.springserver.margin.service.MarginService;
import growup.spring.springserver.marginforcampaign.service.MarginForCampaignService;
import growup.spring.springserver.memo.service.MemoService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

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
    private final LazySegmentTreeService lazySegmentTreeService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public int deleteCampaign(String email, List<Long> campaignIds){
        int deleteRows = memoService.deleteMemoByCampaignIds(campaignIds)
                + keywordBidService.deleteKeywordBidsByCampaignIds(campaignIds)
                + exclusionKeywordService.deleteExclusionKeywordByCampaignIds(campaignIds)
                + campaignOptionDetailsService.deleteCampaignOptionDetailsByExecutionIds(campaignIds)
                + executionService.deleteExecutionByCampaignIds(campaignIds)
                + marginService.deleteMarginByCampaignIds(campaignIds)
                + keywordService.deleteKeywordByCampaignIds(campaignIds)
                + marginForCampaignService.deleteMFC(campaignIds)
                + campaignService.deleteCampaign(campaignIds);
        lazySegmentTreeService.removeAllTreeDataByEmail(email);
        return deleteRows;
    }

    @Transactional
    public Map<String,Integer> deleteCampaignDataByPeriod(String email, CampaignDeleteDto campaignDeleteDto){
        //임계값 확인
        boolean checkThreshold = campaignDeleteDto.checkThreshold();
        List<Tuple> extractDeleteData = null;
        if(checkThreshold){
        extractDeleteData =
            keywordService.extractDeleteCampaignDataByPeriod(campaignDeleteDto.getStart(),campaignDeleteDto.getEnd(),campaignDeleteDto.getCampaignIds());
        }
        Map<String,Integer> result = new HashMap<>();
        result.put("keyword",keywordService.deleteKeywordByCampaignIdsAndDate(campaignDeleteDto.getCampaignIds(),campaignDeleteDto.getStart(),campaignDeleteDto.getEnd()));
        result.put("margin",marginService.deleteKeywordByCampaignIdsAndDate(campaignDeleteDto.getCampaignIds(),campaignDeleteDto.getStart(),campaignDeleteDto.getEnd()));
        List<Long> executionIds = new ArrayList<>();
        for(Long campaignId : campaignDeleteDto.getCampaignIds()){
            executionIds.addAll(executionService.getMyExecutionData(campaignId).stream().map(ExecutionMarginResDto::getExeId).toList());
        }
        result.put("campaignOptionDetail",campaignOptionDetailsService.deleteKeywordByExecutionIdsAndDate(executionIds,campaignDeleteDto.getStart(),campaignDeleteDto.getEnd()));
        result.put("memo",memoService.deleteKeywordByCampaignIdsAndDate(campaignDeleteDto.getCampaignIds(),campaignDeleteDto.getStart(),campaignDeleteDto.getEnd()));
        if(checkThreshold && extractDeleteData != null){
            applicationEventPublisher.publishEvent(new TreeUpdateEvent(email,extractDeleteData));
        }else{
            int year = campaignDeleteDto.getStart().getYear();
            applicationEventPublisher.publishEvent(new TreeBuildEvent(email,year));
        }
        return result;
    }
}
