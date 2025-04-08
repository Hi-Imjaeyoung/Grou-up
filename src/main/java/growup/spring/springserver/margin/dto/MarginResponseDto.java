package growup.spring.springserver.margin.dto;

import growup.spring.springserver.margin.domain.Margin;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MarginResponseDto {
    private Long campaignId;
    private List<MarginResultDto> data;
}