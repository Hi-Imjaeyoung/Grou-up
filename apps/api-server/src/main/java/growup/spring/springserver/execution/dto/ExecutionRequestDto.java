package growup.spring.springserver.execution.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRequestDto {
    @NotNull
    Long campaignId;

    @Size(min = 1)
    List<Long> exeIds;
}
