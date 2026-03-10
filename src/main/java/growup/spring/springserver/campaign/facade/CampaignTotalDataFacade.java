package growup.spring.springserver.campaign.facade;

import growup.spring.springserver.campaign.dto.CampaignAnalysisDto;
import growup.spring.springserver.campaign.dto.TotalCampaignsData;
import growup.spring.springserver.global.cache.AllCampaignTypeData;
import growup.spring.springserver.global.cache.LazySegmentTreeService;
import growup.spring.springserver.global.cache.SegTreeCacheManager;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import growup.spring.springserver.keyword.service.KeywordService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

@Component
@AllArgsConstructor
@Slf4j
public class CampaignTotalDataFacade {

    private final KeywordService keywordService;
    private final SegTreeCacheManager segTreeCacheManager;
    private final LazySegmentTreeService lazySegmentTreeService;

    public TotalCampaignsData getCampaignTotalDataByLazyLoadingTree(String email, LocalDate start, LocalDate end){
        AllCampaignTypeData allCampaignTypeData = lazySegmentTreeService.getCachedOrSelectAllCampaignTypeDataByPeriod(email,start,end);
//        Map<String,CampaignAnalysisDto> campaignAnalysisDataKeyCampaignName =
//                keywordService.getEachCampaignAdCostSumAndAdSalesByPeriodAndEmail(email,start,end);
        return TotalCampaignsData.builder()
//                .adSalesAndAdCostByCampaignName(new HashMap<>(campaignAnalysisDataKeyCampaignName))
                .adSalesAndAdCostByCampaignName(new HashMap<>())
                .sumOfAdSalesAndAdCostByCampaignType(new HashMap<>(allCampaignTypeData.getCampaignAnalysisDtoMap()))
                .build();
    }

    public TotalCampaignsData getCampaignTotalData(String email, LocalDate start, LocalDate end){
        lazySegmentTreeService.incrementRequestCount();
        Map<String, CampaignAnalysisDto> campaignAnalysisDataKeyCampaignType =
                keywordService.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmail(email,start,end);
//        Map<String,CampaignAnalysisDto> campaignAnalysisDataKeyCampaignName =
//                keywordService.getEachCampaignAdCostSumAndAdSalesByPeriodAndEmail(email,start,end);
        return TotalCampaignsData.builder()
//                .adSalesAndAdCostByCampaignName(campaignAnalysisDataKeyCampaignName)
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
