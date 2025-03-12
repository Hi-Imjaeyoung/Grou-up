package growup.spring.springserver.margin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class MarginUpdateRequestDto {

    private Long id;
    private LocalDate marDate;
    private Double marTargetEfficiency;  // 목표 효율성
    private Double marAdBudget;  // 광고 예산
}