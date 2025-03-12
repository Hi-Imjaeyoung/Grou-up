package growup.spring.springserver.margin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarginUpdateResponseDto {
    private int requestNumber; // 요청한 상품의 총 개수
    private int responseNumber; // 성공한 상품의 개수

    private Map<LocalDate, Map<String, Double>> failedDate; // 실패한 상품 이름 리스트
}