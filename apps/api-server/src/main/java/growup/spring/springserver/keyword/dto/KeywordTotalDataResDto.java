package growup.spring.springserver.keyword.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
public class KeywordTotalDataResDto {
    private Map<LocalDate,KeywordResponseDto> search;
    private Map<LocalDate,KeywordResponseDto> nonSearch;
}
