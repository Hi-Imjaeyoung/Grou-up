package growup.spring.springserver.keyword.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import growup.spring.springserver.global.domain.CoupangExcelData;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeywordResponseDto extends CoupangExcelData {

    private String keyKeyword;  // 키워드

    private Boolean keyExcludeFlag;// 제외여부

    private Boolean keyBidFlag; // 수동입찰가 등록 여부

    private String keySearchType;// 검색 비검색

    private Long bid;

    private Map<String,Long> keySalesOptions;
}
