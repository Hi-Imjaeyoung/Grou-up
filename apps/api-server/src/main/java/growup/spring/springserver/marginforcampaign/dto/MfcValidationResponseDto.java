package growup.spring.springserver.marginforcampaign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfcValidationResponseDto {
    private int requestNumber; // 요청한 상품의 총 개수
    private int responseNumber; // 성공한 상품의 개수
    private List<String> failedProductNames; // 실패한 상품 이름 리스트
}