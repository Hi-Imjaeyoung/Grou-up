package growup.spring.springserver.execution.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecutionMarginResDto {
    private Long exeId;
    private String exeDetailCategory;
    private Long exeSalePrice; // 판매가
    private Long exeTotalPrice; // 총 비용
    private Long exeCostPrice; // 원가
}