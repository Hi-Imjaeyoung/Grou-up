package growup.spring.springserver.exclusionKeyword.dto;
;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.springframework.lang.NonNull;

import java.util.List;

@Data
@Builder
public class ExclusionKeywordRequestDto {
    @NotNull(message = "조회할 캠패인 ID를 입력해주세요")
    Long campaignId;

    @Size(min = 1, message = "제외 키워드는 최소 1개 이상이어야 합니다.")
    List<String> exclusionKeyword;
}
