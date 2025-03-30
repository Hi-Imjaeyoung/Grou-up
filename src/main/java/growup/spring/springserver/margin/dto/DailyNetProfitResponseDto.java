package growup.spring.springserver.margin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
public class DailyNetProfitResponseDto {
    private LocalDate marDate;  // 날짜
    private Double margin; // 마진 (캠페인 마다 마진의 총 합)
    private Double marReturnCost;
}