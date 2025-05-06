package growup.spring.springserver.margin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class SimpleMarginResponseDto {
    private Long id;
    private LocalDate marDate;  // 날짜
    private Double marTargetEfficiency;  // 목표 효율성
    private Double marAdBudget;  // 광고 예산
}

