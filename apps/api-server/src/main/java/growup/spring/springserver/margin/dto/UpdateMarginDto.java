package growup.spring.springserver.margin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
public class UpdateMarginDto {
    private LocalDate marDate;  // 날짜
    private Double marAdCost;   // 총 광고비
    private Double marSales;    // 총 매출
    private Double marRoas;     // ROAS (계산된 값)
}