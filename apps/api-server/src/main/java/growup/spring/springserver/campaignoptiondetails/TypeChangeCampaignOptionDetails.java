package growup.spring.springserver.campaignoptiondetails;

import growup.spring.springserver.campaignoptiondetails.domain.CampaignOptionDetails;
import growup.spring.springserver.campaignoptiondetails.dto.CampaignOptionDetailsResponseDto;

public class TypeChangeCampaignOptionDetails {

    public static CampaignOptionDetailsResponseDto entityToResponseDto(CampaignOptionDetails campaignOptionDetails) {
        return CampaignOptionDetailsResponseDto.builder()
                .id(campaignOptionDetails.getExecution().getExeId()) // 옵션아이디
                .name(campaignOptionDetails.getExecution().getExeProductName()) // 상품명
                .copSales(campaignOptionDetails.getCopSales()) // 주문수
                .copAdcost(campaignOptionDetails.getCopAdcost()) // 광고비
                .copAdsales(campaignOptionDetails.getCopAdsales()) // 광고매출
                .copRoas(campaignOptionDetails.getCopRoas()) // ROAS 계산 필요
                .copImpressions(campaignOptionDetails.getCopImpressions()) // 노출 수
                .copClicks(campaignOptionDetails.getCopClicks()) // 클릭수
                .copClickRate(campaignOptionDetails.getCopClickRate()) // 클릭률 계산필요
                .copCvr(campaignOptionDetails.getCopCvr()) // 전환률 계산필요
                .build();
    }
}