package growup.spring.springserver.execution.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ExecutionRequestDtos {
    @NotNull(message = "캠패인 ID를 입력해주세요.")
    private Long campaignId;
    @Size(min = 1, message = " 1개 이상 수정 해주세요.")
    private List<ExecutionDto> data;
}