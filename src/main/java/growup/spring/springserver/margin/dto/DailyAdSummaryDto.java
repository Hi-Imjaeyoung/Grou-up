package growup.spring.springserver.margin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
public class DailyAdSummaryDto {
    private LocalDate marDate;  // 날짜
    private Double marSales;   // 총 광고비
    private Double marNetProfit;    // 총 매출
}