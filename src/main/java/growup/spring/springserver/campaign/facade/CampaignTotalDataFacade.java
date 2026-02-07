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

    private static final int START_ROOT_COUNT = 1;
    private static final int END_ROOT_COUNT = 365;

    public TotalCampaignsData getCampaignTotalDataByCache(String email, LocalDate start, LocalDate end){
        Map<String, CampaignAnalysisDto> campaignAnalysisDataKeyCampaignType = Map.of();
        try{
            AllCampaignTypeData data = segTreeCacheManager.checkPeriod(email,start,end);
            if(data == null) {
                log.debug("캐싱된 데이터를 찾지못했습니다");
                throw new GrouException(ErrorCode.EXECUTION_REQUEST_ERROR);
            }
            log.info("캐싱된 데이터를 로드하였습니다.");
            campaignAnalysisDataKeyCampaignType = data.getCampaignAnalysisDtoMap();
        }catch (GrouException e){
            log.info("Cant Find Cache Data. Go Query");
            segTreeCacheManager.init(email);
            segTreeCacheManager.setRootKey(email,start,end);
            campaignAnalysisDataKeyCampaignType =
                    keywordService.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(email,start,end);
            log.info("build tree");
            segTreeCacheManager.buildTree(email);
        }
        Map<String,CampaignAnalysisDto> campaignAnalysisDataKeyCampaignName =
                keywordService.getEachCampaignAdCostSumAndAdSalesByPeriodAndEmail(email,start,end);
        return TotalCampaignsData.builder()
                .adSalesAndAdCostByCampaignName(campaignAnalysisDataKeyCampaignName)
                .sumOfAdSalesAndAdCostByCampaignType(campaignAnalysisDataKeyCampaignType)
                .build();
    }

    public TotalCampaignsData getCampaignTotalDataByLazyLoadingTree(String email, LocalDate start, LocalDate end){
        // only start year same to end year
        int startCount = lazySegmentTreeService.convertLocalDateToCount(start);
        int endCount = lazySegmentTreeService.convertLocalDateToCount(end);

        AllCampaignTypeData allCampaignTypeData;
        if(start.getYear() != end.getYear()){
            AllCampaignTypeData allCampaignTypeDataPreYear =
                    lazySegmentTreeService.find(email,start.getYear(),START_ROOT_COUNT,END_ROOT_COUNT,startCount,END_ROOT_COUNT);
            AllCampaignTypeData allCampaignTypeDataPostYear =
                    lazySegmentTreeService.find(email,end.getYear(),START_ROOT_COUNT,END_ROOT_COUNT,START_ROOT_COUNT,endCount);
            allCampaignTypeData = allCampaignTypeDataPreYear.sum(allCampaignTypeDataPostYear);

        }else{
             allCampaignTypeData =
                    lazySegmentTreeService.find(email,start.getYear(),START_ROOT_COUNT,END_ROOT_COUNT,startCount,endCount);
        }

//        Map<String,CampaignAnalysisDto> campaignAnalysisDataKeyCampaignName =
//                keywordService.getEachCampaignAdCostSumAndAdSalesByPeriodAndEmail(email,start,end);

        return TotalCampaignsData.builder()
                .adSalesAndAdCostByCampaignName(new HashMap<>())
                .sumOfAdSalesAndAdCostByCampaignType(new HashMap<>(allCampaignTypeData.getCampaignAnalysisDtoMap()))
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
