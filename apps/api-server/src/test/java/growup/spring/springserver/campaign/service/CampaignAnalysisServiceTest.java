package growup.spring.springserver.campaign.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.dto.CampaignAnalysisDto;
import growup.spring.springserver.campaign.dto.TotalCampaignsData;
import growup.spring.springserver.keyword.domain.Keyword;
import growup.spring.springserver.keyword.dto.KeywordTotalDataResDto;
import growup.spring.springserver.keyword.service.KeywordQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class CampaignAnalysisServiceTest {
    @InjectMocks
    private CampaignAnalysisService campaignAnalysisService;

    @Mock
    private KeywordQueryService keywordQueryService;

    @Test
    @DisplayName("findAllByDateANDCampaign 성공")
    void getTotalData_test1(){
        doReturn(getKeywordList()).when(keywordQueryService).findAllByDateANDCampaign(any(LocalDate.class),any(LocalDate.class),any(Long.class));
        KeywordTotalDataResDto keywordTotalDataResDto = campaignAnalysisService.getTotalData(LocalDate.now(),LocalDate.now(),1L);
        assertThat(keywordTotalDataResDto.getSearch().get(LocalDate.now()).getAdCost()).isEqualTo(100L);
        assertThat(keywordTotalDataResDto.getNonSearch().get(LocalDate.now()).getAdCost()).isEqualTo(90L);
    }

    @Test
    @DisplayName("findAllByDateANDCampaign에서 빈값을 받을 때")
    void getTotalData_test2(){
        doReturn(List.of()).when(keywordQueryService).findAllByDateANDCampaign(any(LocalDate.class),any(LocalDate.class),any(Long.class));
        KeywordTotalDataResDto keywordTotalDataResDto = campaignAnalysisService.getTotalData(LocalDate.now(),LocalDate.now(),1L);
        assertThat(keywordTotalDataResDto.getSearch().isEmpty()).isTrue();
        assertThat(keywordTotalDataResDto.getNonSearch().isEmpty()).isTrue();
    }

    @Test
    void calculateAdCostAndAdSales_test1(){
        CampaignAnalysisDto aTypeCampaign = CampaignAnalysisDto.builder()
                .adSales(10.0)
                .adCost(10.0)
                .build();
        CampaignAnalysisDto totalTypeCampaign = CampaignAnalysisDto.builder()
                .adSales(20.0)
                .adCost(20.0)
                .build();
        CampaignAnalysisDto bTypeCampaign = campaignAnalysisService.calculateAdCostAndAdSales(aTypeCampaign,totalTypeCampaign,
                1L,
                "bType",
                getKeywordList());
        // 기존 aType adCost + keywordList의 adCost의 총합 190 == 200L
        assertThat(aTypeCampaign.getAdCost()).isEqualTo(200L);
        // 기존 totalType adCost + keywordList의 adCost의 총합 190 == 210L
        assertThat(totalTypeCampaign.getAdCost()).isEqualTo(210L);
        // keywordList의 adCost의 총합 190 == 190L
        assertThat(bTypeCampaign.getAdCost()).isEqualTo(190L);
        assertThat(bTypeCampaign.getCampaignId()).isEqualTo(1L);
        assertThat(bTypeCampaign.getCampAdType()).isEqualTo("bType");
    }

    @Test
    @DisplayName("getKeywordList에서 빈리스트를 받을 경우")
    void calculateAdCostAndAdSales_test2(){
        CampaignAnalysisDto aTypeCampaign = CampaignAnalysisDto.builder()
                .adSales(10.0)
                .adCost(10.0)
                .build();
        CampaignAnalysisDto totalTypeCampaign = CampaignAnalysisDto.builder()
                .adSales(20.0)
                .adCost(20.0)
                .build();
        CampaignAnalysisDto bTypeCampaign = campaignAnalysisService.calculateAdCostAndAdSales(aTypeCampaign,totalTypeCampaign,
                1L,
                "bType",
                List.of());
        assertThat(aTypeCampaign.getAdCost()).isEqualTo(10L);
        assertThat(totalTypeCampaign.getAdCost()).isEqualTo(20L);
        assertThat(bTypeCampaign.getAdCost()).isEqualTo(0L);
        assertThat(bTypeCampaign.getCampaignId()).isEqualTo(1L);
        assertThat(bTypeCampaign.getCampAdType()).isEqualTo("bType");
    }

    @Test
    void getMyAllCampaignsDataByDate_test1(){
        doReturn(getKeywordList(1L)).when(keywordQueryService).findAllByDateANDCampaign(LocalDate.now().minusDays(1L),LocalDate.now(),1L);
        doReturn(getKeywordList(2L)).when(keywordQueryService).findAllByDateANDCampaign(LocalDate.now().minusDays(1L),LocalDate.now(),2L);
        doReturn(getKeywordList(3L)).when(keywordQueryService).findAllByDateANDCampaign(LocalDate.now().minusDays(1L),LocalDate.now(),3L);
        TotalCampaignsData totalCampaignsData = campaignAnalysisService.getMyAllCampaignsDataByDate(LocalDate.now().minusDays(1L),LocalDate.now(),getCampaignList());

        assertThat(totalCampaignsData.getSumOfAdSalesAndAdCostByCampaignType().get("총 매출").getAdCost()).isEqualTo(570);
        assertThat(totalCampaignsData.getSumOfAdSalesAndAdCostByCampaignType().get("type1").getAdCost()).isEqualTo(190L);
        assertThat(totalCampaignsData.getSumOfAdSalesAndAdCostByCampaignType().get("type2").getAdCost()).isEqualTo(190L);
        assertThat(totalCampaignsData.getSumOfAdSalesAndAdCostByCampaignType().get("type3").getAdCost()).isEqualTo(190L);
        assertThat(totalCampaignsData.getAdSalesAndAdCostByCampaignName().get("cam1").getAdCost()).isEqualTo(190L);
        assertThat(totalCampaignsData.getAdSalesAndAdCostByCampaignName().get("cam2").getAdSales()).isEqualTo(190L);
        assertThat(totalCampaignsData.getAdSalesAndAdCostByCampaignName().get("cam3").getAdCost()).isEqualTo(190L);
    }

    @Test
    @DisplayName("getKeywordList에서 빈리스트를 받을 경우")
    void getMyAllCampaignsDataByDate_test2(){
        doReturn(List.of()).when(keywordQueryService).findAllByDateANDCampaign(LocalDate.now().minusDays(1L),LocalDate.now(),1L);
        doReturn(List.of()).when(keywordQueryService).findAllByDateANDCampaign(LocalDate.now().minusDays(1L),LocalDate.now(),2L);
        doReturn(List.of()).when(keywordQueryService).findAllByDateANDCampaign(LocalDate.now().minusDays(1L),LocalDate.now(),3L);
        TotalCampaignsData totalCampaignsData = campaignAnalysisService.getMyAllCampaignsDataByDate(LocalDate.now().minusDays(1L),LocalDate.now(),getCampaignList());

        assertThat(totalCampaignsData.getSumOfAdSalesAndAdCostByCampaignType().get("총 매출").getAdCost()).isEqualTo(0);
        assertThat(totalCampaignsData.getSumOfAdSalesAndAdCostByCampaignType().get("type1").getAdCost()).isEqualTo(0);
        assertThat(totalCampaignsData.getSumOfAdSalesAndAdCostByCampaignType().get("type2").getAdCost()).isEqualTo(0);
        assertThat(totalCampaignsData.getSumOfAdSalesAndAdCostByCampaignType().get("type3").getAdCost()).isEqualTo(0);
        assertThat(totalCampaignsData.getAdSalesAndAdCostByCampaignName().get("cam1").getAdCost()).isEqualTo(0);
        assertThat(totalCampaignsData.getAdSalesAndAdCostByCampaignName().get("cam2").getAdSales()).isEqualTo(0);
        assertThat(totalCampaignsData.getAdSalesAndAdCostByCampaignName().get("cam3").getAdCost()).isEqualTo(0);
    }

    public List<Campaign> getCampaignList(){
        return List.of(Campaign.builder().campaignId(1L).camCampaignName("cam1").camAdType("type1").build(),
                Campaign.builder().campaignId(2L).camCampaignName("cam2").camAdType("type2").build(),
                Campaign.builder().campaignId(3L).camCampaignName("cam3").camAdType("type3").build());

    }
    public List<Keyword> getKeywordList(Long campaignId ){
        List<Keyword> list = new ArrayList<>();
        for(int i=0;i<20;i++){
            Keyword keyword;
            if(i%2 ==0){
                keyword = Keyword.builder()
                        .keyKeyword("-")
                        .clicks((long)i)
                        .impressions((long) i)
                        .totalSales((long) i)
                        .adSales((double) i)
                        .adCost((double) i)
                        .date(LocalDate.now().minusDays(1L))
                        .build();
            }else{
                keyword = Keyword.builder()
                        .keyKeyword("keyword"+i+"campaignId"+campaignId)
                        .clicks((long)i)
                        .impressions((long) i)
                        .totalSales((long) i)
                        .adSales((double) i)
                        .adCost((double) i)
                        .date(LocalDate.now())
                        .build();
            }

            list.add(keyword);
        }
        return list;
    }
    public List<Keyword> getKeywordList(){
        List<Keyword> list = new ArrayList<>();
        for(int i=0;i<20;i++){
            Keyword keyword;
            if(i%2 ==0){
                keyword = Keyword.builder()
                        .keyKeyword("-")
                        .clicks((long)i)
                        .impressions((long) i)
                        .totalSales((long) i)
                        .adSales((double) i)
                        .adCost((double) i)
                        .date(LocalDate.now())
                        .build();
            }else{
                keyword = Keyword.builder()
                        .keyKeyword("keyword"+i)
                        .clicks((long)i)
                        .impressions((long) i)
                        .totalSales((long) i)
                        .adSales((double) i)
                        .adCost((double) i)
                        .date(LocalDate.now())
                        .build();
            }

            list.add(keyword);
        }
        return list;
    }
}
