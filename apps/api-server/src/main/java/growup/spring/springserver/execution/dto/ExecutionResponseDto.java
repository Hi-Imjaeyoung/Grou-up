package growup.spring.springserver.execution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResponseDto {
    private Long exeId;
    private String exeProductName;
    private int requestNumber; // 요청갯수
    private int responseNumber; // 응답갯수
}
