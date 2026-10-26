package growup.spring.springserver.campaign.facade;

import growup.spring.springserver.campaign.dto.CampaignAnalysisDto;
import growup.spring.springserver.campaign.dto.TotalCampaignsData;
import growup.spring.springserver.global.cache.*;
import growup.spring.springserver.global.listener.TreeBuildEvent;
import growup.spring.springserver.keyword.service.KeywordService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class CampaignTotalDataFacade {

    private final KeywordService keywordService;
    private final LazySegmentTreeService lazySegmentTreeService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public TotalCampaignsData getCampaignTotalDataByLazyLoadingTree(String email, LocalDate start, LocalDate end){
        Map<String,CampaignAnalysisDto> campaignAnalysisDataKeyCampaignName =
                keywordService.getEachCampaignAdCostSumAndAdSalesByPeriodAndEmail(email,start,end);
        AllCampaignTypeData allCampaignTypeData;
        if(lazySegmentTreeService.isTreeBuild(email,start.getYear())){
            allCampaignTypeData =
                    lazySegmentTreeService.getCachedOrSelectAllCampaignTypeDataByPeriod(email,start,end);
            return TotalCampaignsData.builder()
                    .adSalesAndAdCostByCampaignName(new HashMap<>(campaignAnalysisDataKeyCampaignName))
                    .sumOfAdSalesAndAdCostByCampaignType(new HashMap<>(allCampaignTypeData.getCampaignAnalysisDtoMap()))
                    .build();
        }
        eventPublisher.publishEvent(new TreeBuildEvent(email,start.getYear()));
        if(end.getYear() != start.getYear()) eventPublisher.publishEvent(new TreeBuildEvent(email,start.getYear()));
        Map<String, CampaignAnalysisDto> campaignAnalysisDataKeyCampaignType =
                keywordService.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmail(email,start,end);
        return TotalCampaignsData.builder()
                .adSalesAndAdCostByCampaignName(campaignAnalysisDataKeyCampaignName)
                .sumOfAdSalesAndAdCostByCampaignType(campaignAnalysisDataKeyCampaignType)
                .build();
    }

    public TotalCampaignsData getCampaignTotalData(String email, LocalDate start, LocalDate end){
        lazySegmentTreeService.incrementRequestCount();
        Map<String, CampaignAnalysisDto> campaignAnalysisDataKeyCampaignType =
                keywordService.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmail(email,start,end);
        Map<String,CampaignAnalysisDto> campaignAnalysisDataKeyCampaignName =
                keywordService.getEachCampaignAdCostSumAndAdSalesByPeriodAndEmail(email,start,end);
        return TotalCampaignsData.builder()
                .adSalesAndAdCostByCampaignName(campaignAnalysisDataKeyCampaignName)
                .adSalesAndAdCostByCampaignName(new HashMap<>())
                .sumOfAdSalesAndAdCostByCampaignType(campaignAnalysisDataKeyCampaignType)
                .build();
    }

    public Map<String,Long> getCacheHitRate(){
        return lazySegmentTreeService.getCacheStats();
    }

    public void resetCacheStats(){
        lazySegmentTreeService.resetCacheStats();
    }
}
