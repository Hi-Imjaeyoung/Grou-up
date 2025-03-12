package growup.spring.springserver.marginforcampaign.dto;

import growup.spring.springserver.marginforcampaign.support.MarginType;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MfcDto {
    private Long mfcId;
    private String mfcProductName; // 상품명
    private Long mfcSalePrice;
    private Long mfcTotalPrice; // 총비용
    private MarginType mfcType;
    private Long mfcReturnPrice;
    private Long mfcCostPrice; // 원가
    private Long mfcPerPiece; //  1개당 마진
    private Double mfcZeroRoas; // 제로 Roas
}