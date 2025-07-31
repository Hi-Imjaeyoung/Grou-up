package growup.spring.springserver.campaign.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.dto.CampaignAnalysisDto;
import growup.spring.springserver.campaign.dto.TotalCampaignsData;
import growup.spring.springserver.keyword.TypeChangeKeyword;
import growup.spring.springserver.keyword.domain.Keyword;
import growup.spring.springserver.keyword.dto.KeywordResponseDto;
import growup.spring.springserver.keyword.dto.KeywordTotalDataResDto;
import growup.spring.springserver.keyword.repository.KeywordRepository;
import growup.spring.springserver.keyword.service.KeywordQueryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class CampaignAnalysisService {
    private static final String TOTAL_SALE_KEY = "총 매출";
    private final KeywordQueryService keywordQueryService;

    // 날짜 범위 내 캠패인의 검색, 비검색 구분하여 데이터 조회.
    public KeywordTotalDataResDto getTotalData(LocalDate start,
                                               LocalDate end,
                                               Long campaignId){
        List<Keyword> data = keywordQueryService.findAllByDateANDCampaign(start,end,campaignId);
        Map<LocalDate, KeywordResponseDto> search = new HashMap<>();
        Map<LocalDate,KeywordResponseDto> nonSearch = new HashMap<>();
        for(Keyword keyword : data){
            //nonSearch
            if(keyword.getKeyKeyword().equals("-")){
                if(nonSearch.containsKey(keyword.getDate())){
                    nonSearch.get(keyword.getDate()).update(keyword);
                    continue;
                }
                KeywordResponseDto keywordResponseDto = TypeChangeKeyword.entityToResponseDto(keyword);
                keywordResponseDto.calculatePercentData();
                nonSearch.put(keyword.getDate(), keywordResponseDto);
                continue;
            }
            //search
            if(search.containsKey(keyword.getDate())){
                search.get(keyword.getDate()).update(keyword);
                continue;
            }
            search.put(keyword.getDate(),TypeChangeKeyword.entityToResponseDto(keyword));
        }
        return KeywordTotalDataResDto.builder()
                .search(search)
                .nonSearch(nonSearch)
                .build();
    }
    public TotalCampaignsData getMyAllCampaignsDataByDate(LocalDate start, LocalDate end, List<Campaign> campaigns){
        Map<String,CampaignAnalysisDto> sumOfAdSalesAndAdCostByCampaignType = new HashMap<>();
        Map<String,CampaignAnalysisDto> adSalesAndAdCostByCampaignName = new HashMap<>();
        sumOfAdSalesAndAdCostByCampaignType.put(TOTAL_SALE_KEY,CampaignAnalysisDto.builder()
                .adCost(0.0)
                .adSales(0.0)
                .build());
        for(Campaign campaign : campaigns){
            List<Keyword> keywordList = keywordQueryService.findAllByDateANDCampaign(start,end, campaign.getCampaignId());
            if(!sumOfAdSalesAndAdCostByCampaignType.containsKey(campaign.getCamAdType())){
                sumOfAdSalesAndAdCostByCampaignType.put(campaign.getCamAdType(),CampaignAnalysisDto.builder()
                        .adCost(0.0)
                        .adSales(0.0)
                        .build());
            }
            adSalesAndAdCostByCampaignName.put(campaign.getCamCampaignName(),
                    calculateAdCostAndAdSales(sumOfAdSalesAndAdCostByCampaignType.get(campaign.getCamAdType())
                            ,sumOfAdSalesAndAdCostByCampaignType.get(TOTAL_SALE_KEY)
                            ,campaign.getCampaignId()
                            ,campaign.getCamAdType()
                            ,keywordList));
        }

        return TotalCampaignsData.builder()
                .sumOfAdSalesAndAdCostByCampaignType(sumOfAdSalesAndAdCostByCampaignType)
                .adSalesAndAdCostByCampaignName(adSalesAndAdCostByCampaignName)
                .build();
    }

    public CampaignAnalysisDto calculateAdCostAndAdSales(CampaignAnalysisDto campaignAnalysisDto,
                                                         CampaignAnalysisDto totalCampaignAnalysisDto,
                                                         Long campaignId,
                                                         String campaignAdType,
                                                         List<Keyword> keywordList){
        // 파라미터로 넘어온 값은 캠패인 타입에 따른 전체 데이터의 합한 값
        CampaignAnalysisDto newCampaignAnalysisDto = CampaignAnalysisDto.builder()
                .adSales(0.0)
                .adCost(0.0)
                .campaignId(campaignId)
                .campAdType(campaignAdType)
                .build();
        for(Keyword keyword : keywordList){
            totalCampaignAnalysisDto.plusAdSales(keyword.getAdSales());
            totalCampaignAnalysisDto.plusAdCost(keyword.getAdCost());
            campaignAnalysisDto.plusAdCost(keyword.getAdCost());
            campaignAnalysisDto.plusAdSales(keyword.getAdSales());
            newCampaignAnalysisDto.plusAdCost(keyword.getAdCost());
            newCampaignAnalysisDto.plusAdSales(keyword.getAdSales());
        }
        return newCampaignAnalysisDto;
    }
}
