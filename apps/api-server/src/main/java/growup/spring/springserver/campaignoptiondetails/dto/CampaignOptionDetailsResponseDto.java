package growup.spring.springserver.campaignoptiondetails.dto;

import growup.spring.springserver.campaignoptiondetails.domain.CampaignOptionDetails;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CampaignOptionDetailsResponseDto {
    private Long id;
    private String name; // 옵션명
    private Long copSales;
    private Long copImpressions;
    private Double copAdcost;
    private Double copAdsales;
    private Double copRoas;
    private Long copClicks;
    private Double copClickRate;
    private Double copCvr;

    public void update(CampaignOptionDetails details) {
        // 누적 합산
        copSales += details.getCopSales();
        copAdcost += details.getCopAdcost();
        copAdsales += details.getCopAdsales();
        copImpressions += details.getCopImpressions();
        copClicks += details.getCopClicks();

        // 계산 로직
        if (copAdsales != 0) copRoas = (double) (Math.round((copAdsales / copAdcost) * 10000)) / 100.0;
        if (copImpressions != 0) copClickRate = Math.round((double) copClicks / copImpressions * 10000) / 100.0;
        if (copClicks != 0) copCvr = Math.round((double) copSales / copClicks * 10000) / 100.0;
    }
}
