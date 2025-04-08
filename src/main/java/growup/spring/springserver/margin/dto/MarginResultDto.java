package growup.spring.springserver.margin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MarginResultDto {
    private Long id;
    private LocalDate marDate;  // 날짜
    private Long marImpressions;  // 노출 수
    private Long marClicks;  // 클릭 수 -> 클릭률 구해야함
    private Long marAdConversionSales;  // 광고 전환 판매 수
    private Long marAdConversionSalesCount ; // 광고 전환 주문수 (CVR 계산 용)
    private Long marReturnCount; // 반품 갯수
    private Double marReturnCost; // 반품 비용
    private Double marAdCost;
    private Double marSales;
    private Long marAdMargin;  // 광고 머진 계산필요
    private Double marNetProfit;  // 순이익
    private Double marTargetEfficiency;  // 목표 효율성
    private Double marAdBudget;  // 광고 예산
    private Long marActualSales;  // 실제 판매 수
}