package growup.spring.springserver.margin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class MarginOverviewResponseDto {
    private Long campaignId;
    private String campaignName;  // 캠페인 이름
    private Double marSales; // 매출
    private Double marNetProfit; // 순이익

    private Double marMarginRate ;// 마진율
    // 마진율: (순이익/매출) * 100
    private Double marRoi; // ROI
    // 순이익 / 집행광고비 * 100

    private Double marAdCost; // 광고비
    private Long marReturnCount; // 반품 갯수
    private Double marReturnCost; // 반품 비용
    private Long marAdConversionSalesCount; // 광고 전환 주문수
    private Double marReturnRate; // 반품률
    // 반품률: (반품갯수/광고전환주문수) * 100

}