package growup.spring.springserver.margin.dto;

import growup.spring.springserver.global.domain.CoupangExcelData;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Optional;

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

    public void plusData(CoupangExcelData coupangExcelData) {
        if (coupangExcelData == null) {
            return;
        }
        this.marImpressions += Optional.ofNullable(coupangExcelData.getImpressions()).orElse(0L);
        this.marClicks += Optional.ofNullable(coupangExcelData.getClicks()).orElse(0L);
        this.marAdConversionSales += Optional.ofNullable(coupangExcelData.getTotalSales()).orElse(0L);
        this.marAdCost += Optional.ofNullable(coupangExcelData.getAdCost()).orElse(0.0);
        this.marSales += Optional.ofNullable(coupangExcelData.getAdSales()).orElse(0.0);
    }
}