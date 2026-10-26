package growup.spring.springserver.marginforcampaignchangedbyperiod;

import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaignchangedbyperiod.domain.MarginForCampaignChangedByPeriod;
import growup.spring.springserver.marginforcampaignchangedbyperiod.dto.MarginChangeSaveRequestDto;

import java.time.LocalDate;

public class TypeChangeMarginForCampaignChangedByPeriod {
    public static MarginForCampaignChangedByPeriod of(MarginChangeSaveRequestDto dto, MarginForCampaign mfc, LocalDate date) {
        return MarginForCampaignChangedByPeriod.builder()
                .date(date)
                .salePrice(dto.salePrice())
                .totalPrice(dto.totalPrice())
                .costPrice(dto.costPrice())
                .returnPrice(dto.returnPrice())
                .marginForCampaign(mfc)
                .build();
    }
}