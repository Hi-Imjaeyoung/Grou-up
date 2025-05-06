package growup.spring.springserver.margin.converter;

import growup.spring.springserver.margin.domain.Margin;



public interface MarginConverter<T> {
    T convert(Margin margin);
}

// 전략 객체는 비즈니스 흐름 제어를 하면 안됨 -> 변환만 책임짐
