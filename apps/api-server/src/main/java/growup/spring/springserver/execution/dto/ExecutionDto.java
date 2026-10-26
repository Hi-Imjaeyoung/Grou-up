package growup.spring.springserver.execution.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ExecutionDto {
    private Long executionId;
    private Long exeSalePrice;
    private Long exeTotalPrice;
    private Long exeCostPrice;
    private Double exePerPiece; //  1개당 마진
    private Double exeZeroRoas; // 제로 Roas
}