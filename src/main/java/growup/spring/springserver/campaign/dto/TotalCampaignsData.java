package growup.spring.springserver.campaign.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
public class TotalCampaignsData {
    //key : campAdType
    Map<String,CampaignAnalysisDto> sumOfAdSalesAndAdCostByCampaignType;

    //key : campaignName
    Map<String,CampaignAnalysisDto> adSalesAndAdCostByCampaignName;
    public TotalCampaignsData(Map<String,CampaignAnalysisDto> map, Map<String,CampaignAnalysisDto> map2){
        this.sumOfAdSalesAndAdCostByCampaignType = map;
        this.adSalesAndAdCostByCampaignName = map2;
    }
}
