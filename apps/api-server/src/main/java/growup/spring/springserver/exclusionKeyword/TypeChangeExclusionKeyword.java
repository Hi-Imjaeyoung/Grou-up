package growup.spring.springserver.exclusionKeyword;

import growup.spring.springserver.exclusionKeyword.domain.ExclusionKeyword;
import growup.spring.springserver.exclusionKeyword.dto.ExclusionKeywordResponseDto;

public class TypeChangeExclusionKeyword {
    public static ExclusionKeywordResponseDto entityToResponseDTO(ExclusionKeyword e){
        return ExclusionKeywordResponseDto.builder()
                .campaignId(e.getCampaign().getCampaignId())
                .exclusionKeyword(e.getExclusionKeyword())
                .addTime(e.getInsDate().toLocalDate())
                .build();
    }
}
