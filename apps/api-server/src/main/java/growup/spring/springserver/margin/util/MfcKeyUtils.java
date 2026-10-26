package growup.spring.springserver.margin.util;

import growup.spring.springserver.margin.dto.MfcKey;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class MfcKeyUtils {

    // 👇 외부에서 인스턴스 생성 못하도록 private 생성자 추가
    private MfcKeyUtils() {
        log.warn("[MfcKeyUtils] Constructor");
        throw new UnsupportedOperationException("Utility class");
    }

    public static <T extends MfcKeyProvider> Map<MfcKey, T> toMfcMap(List<T> dataList) {
        return dataList.stream().collect(Collectors.toMap(
                item -> new MfcKey(item.getMfcProductName(), item.getMfcType()),
                Function.identity()
        ));
    }
}

// 1. getMfcProductName(), getMfcType()을 사용할 수 있게 타입을 보장
// 2. key는 상품명+타입으로 만든 MfcKey, value는 원본 객체