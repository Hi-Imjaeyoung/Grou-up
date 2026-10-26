package growup.spring.springserver.margin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DailyMarginSummary {
    private String marProductName;
    private Long marAdMargin;  // 광고 머진 계산필요
    private Double marNetProfit;
}