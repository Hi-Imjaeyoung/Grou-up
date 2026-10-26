package growup.spring.springserver.keyword;

import growup.spring.springserver.keyword.dto.KeywordResponseDto;
import growup.spring.springserver.keyword.domain.Keyword;

import java.util.HashMap;

public class TypeChangeKeyword {
    public static KeywordResponseDto entityToResponseDto(Keyword keyword){
        return KeywordResponseDto.builder()
                .adCost(keyword.getAdCost())
                .adSales(keyword.getAdSales())
                .keyKeyword(keyword.getKeyKeyword())
                .clicks(keyword.getClicks())
                .cvr(keyword.getCvr())
                .roas(keyword.getRoas())
                .cpc(keyword.getCpc())
                .impressions(keyword.getImpressions())
                .date(keyword.getDate())
                .keyExcludeFlag(false)
                .keyBidFlag(false)
                .clickRate(keyword.getClickRate())
                .keySalesOptions(keyword.getKeyProductSales())
                .keySearchType(keyword.getKeySearchType())
                .totalSales(keyword.getTotalSales())
                .build();
    }
}
