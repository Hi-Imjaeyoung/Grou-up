package growup.spring.springserver.campaign.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CampaignResponseDto {
    String title;
    Long campaignId;
    String camAdType;
}
