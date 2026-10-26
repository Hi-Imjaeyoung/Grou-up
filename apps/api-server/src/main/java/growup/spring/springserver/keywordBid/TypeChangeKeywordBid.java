package growup.spring.springserver.keywordBid;

import growup.spring.springserver.keywordBid.domain.KeywordBid;
import growup.spring.springserver.keywordBid.dto.KeywordBidDto;

public class TypeChangeKeywordBid {
    public static KeywordBidDto entityToDto(KeywordBid keywordBid){
        return KeywordBidDto.builder()
                .keyword(keywordBid.getKeyword())
                .bid(keywordBid.getBid())
                .build();
    }
}
