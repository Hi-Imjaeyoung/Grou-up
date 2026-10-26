package growup.spring.springserver.exclusionKeyword.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExclusionKeywordResponseDto {
    String exclusionKeyword;
    Long campaignId;
    LocalDate addTime;
    int requestData;
    int responseData;
}
