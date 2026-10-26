package growup.spring.springserver.keywordBid.dto;

import growup.spring.springserver.keyword.dto.KeywordResponseDto;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class KeywordBidDto {
    private String keyword;
    private Long bid;
    private KeywordResponseDto keyInfo;
}
