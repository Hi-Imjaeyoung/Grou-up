package growup.spring.springserver.keywordBid.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class KeywordBidRequestDtos {
    @NotNull(message = "캠패인 ID를 입력해주세요")
    private Long campaignId;

    @Size(min = 1, message = "요청 입찰가는 최소 1개 이상이어야 합니다.")
    private List<KeywordBidDto> data;
}
