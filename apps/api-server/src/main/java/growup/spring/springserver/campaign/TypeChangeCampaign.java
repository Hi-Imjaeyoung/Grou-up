package growup.spring.springserver.campaign;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.dto.CampaignResponseDto;

public class TypeChangeCampaign {
    public static CampaignResponseDto entityToResponseDto(Campaign c){
        return CampaignResponseDto.builder()
                .campaignId(c.getCampaignId())
                .camAdType(c.getCamAdType())
                .title(c.getCamCampaignName())
                .build();
    }
}
