package growup.spring.springserver.margin.factory;

import growup.spring.springserver.margin.converter.MarginConverter;
import growup.spring.springserver.margin.converter.MarginToMarginResultConverter;
import growup.spring.springserver.margin.converter.MarginToSimpleMarginConverter;
import growup.spring.springserver.margin.dto.MarginResultDto;
import growup.spring.springserver.margin.dto.SimpleMarginResponseDto;
import org.springframework.stereotype.Component;

@Component // 스프링이 관리, DI를 위해
public class MarginConverterFactory {

    public MarginConverter<MarginResultDto> getResultConverter() {
        return new MarginToMarginResultConverter();
    }

    public MarginConverter<SimpleMarginResponseDto> getSimpleConverter() {
        return new MarginToSimpleMarginConverter();
    }
}