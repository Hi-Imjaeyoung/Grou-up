package growup.spring.springserver.margin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
@Data
@Builder
public class MarginSummaryResponseDto {
    private LocalDate date;
    private Long campaignId;
    private String campaignName;
    private Double yesterdaySales;
    private Double todaySales;
    private Double differentSales;
}