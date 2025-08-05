package growup.spring.springserver.campaign.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignAnalysisDto {
    Double adCost;
    Double adSales;
    String campAdType;
    Long campaignId;
    public void plusAdCost(Double adCost){
        this.adCost += adCost;
    }

    public void plusAdSales (Double adSales){
        this.adSales += adSales;
    }
}
