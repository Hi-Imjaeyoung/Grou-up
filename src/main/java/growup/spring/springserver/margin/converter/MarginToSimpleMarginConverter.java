package growup.spring.springserver.margin.converter;

import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.dto.SimpleMarginResponseDto;

public class MarginToSimpleMarginConverter implements MarginConverter<SimpleMarginResponseDto> {

    @Override
    public SimpleMarginResponseDto convert(Margin margin) {
        return SimpleMarginResponseDto.builder()
                .marDate(margin.getMarDate())
                .marTargetEfficiency(margin.getMarTargetEfficiency())
                .marAdBudget(margin.getMarAdBudget())
                .build();
    }
}
