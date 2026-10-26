package growup.spring.springserver.margin.converter;

import growup.spring.springserver.margin.domain.Margin;



public interface MarginConverter<T> {
    T convert(Margin margin);
}

