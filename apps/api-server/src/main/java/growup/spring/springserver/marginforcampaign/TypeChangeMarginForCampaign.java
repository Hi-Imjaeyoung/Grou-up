package growup.spring.springserver.marginforcampaign;


import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.dto.MarginForCampaignResDto;
import growup.spring.springserver.marginforcampaign.dto.MfcDto;
import growup.spring.springserver.marginforcampaign.dto.MfcValidationResponseDto;

import java.util.List;

public class TypeChangeMarginForCampaign {

    public static MarginForCampaignResDto EntityToDto(MarginForCampaign data) {
        return MarginForCampaignResDto.builder()
                .id(data.getId())
                .mfcProductName(data.getMfcProductName())
                .mfcSalePrice(data.getMfcSalePrice())
                .mfcTotalPrice(data.getMfcTotalPrice())
                .mfcCostPrice(data.getMfcCostPrice())
                .mfcType(data.getMfcType())
                .mfcReturnPrice(data.getMfcReturnPrice())
                .mfcPerPiece(data.getMfcPerPiece())
                .mfcZeroRoas(data.getMfcZeroRoas())
                .build();
    }
    public static MarginForCampaignResDto EntityToDtoWithCampaign(MarginForCampaign data) {
        return MarginForCampaignResDto.builder()
                .id(data.getId())
                .CampaignId(data.getCampaign().getCampaignId())
                .campaignName(data.getCampaign().getCamCampaignName())
                .mfcProductName(data.getMfcProductName())
                .mfcSalePrice(data.getMfcSalePrice())
                .mfcTotalPrice(data.getMfcTotalPrice())
                .mfcCostPrice(data.getMfcCostPrice())
                .mfcType(data.getMfcType())
                .mfcReturnPrice(data.getMfcReturnPrice())
                .mfcPerPiece(data.getMfcPerPiece())
                .mfcZeroRoas(data.getMfcZeroRoas())
                .build();
    }

    public static MfcValidationResponseDto validationResponse(int request, int response, List<String> failedProductNames) {
        return MfcValidationResponseDto.builder()
                .requestNumber(request)
                .responseNumber(response)
                .failedProductNames(failedProductNames)
                .build();
    }

    public static MarginForCampaign createDtoToMargin(MfcDto mfcDto, Campaign campaign) {
        return MarginForCampaign.builder()
                .mfcProductName(mfcDto.getMfcProductName())
                .mfcTotalPrice(mfcDto.getMfcTotalPrice())
                .mfcSalePrice(mfcDto.getMfcSalePrice())
                .mfcCostPrice(mfcDto.getMfcCostPrice())
                .mfcType(mfcDto.getMfcType())
                .mfcReturnPrice(mfcDto.getMfcReturnPrice())
                .mfcPerPiece(mfcDto.getMfcPerPiece())
                .mfcZeroRoas(mfcDto.getMfcZeroRoas())
                .campaign(campaign)
                .build();
    }


}