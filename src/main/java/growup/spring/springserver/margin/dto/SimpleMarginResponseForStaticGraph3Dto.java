package growup.spring.springserver.margin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
public class SimpleMarginResponseForStaticGraph3Dto {
    private Long campaignId;
    private List<SimpleMarginResponseDto> data;
}


