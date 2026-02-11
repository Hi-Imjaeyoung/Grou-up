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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class CampaignTotalDataFacade {

    private final KeywordService keywordService;
    private final SegTreeCacheManager segTreeCacheManager;
    private final LazySegmentTreeService lazySegmentTreeService;

    @Transactional(readOnly = true)
    public TotalCampaignsData getCampaignTotalDataByLazyLoadingTree(String email, LocalDate start, LocalDate end){
        // only start year same to end year
        int startCount = lazySegmentTreeService.convertLocalDateToCount(start);
        int endCount = lazySegmentTreeService.convertLocalDateToCount(end);
        Map<String,CampaignAnalysisDto> campaignAnalysisDataKeyCampaignName =
                keywordService.getEachCampaignAdCostSumAndAdSalesByPeriodAndEmail(email,start,end);
        AllCampaignTypeData allCampaignTypeData;
        if(lazySegmentTreeService.isTreeBuild(email,start.getYear())){
            allCampaignTypeData =
                    lazySegmentTreeService.find(email,start.getYear(),START_ROOT_COUNT,END_ROOT_COUNT,startCount,endCount);
            return TotalCampaignsData.builder()
                    .adSalesAndAdCostByCampaignName(new HashMap<>())
                    .sumOfAdSalesAndAdCostByCampaignType(new HashMap<>(allCampaignTypeData.getCampaignAnalysisDtoMap()))
                    .build();
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                lazySegmentTreeService.buildTreeAsync(email,start.getYear());
            }
        });
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
