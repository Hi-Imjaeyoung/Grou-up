package growup.spring.springserver.netsales.dto;

import growup.spring.springserver.marginforcampaign.support.MarginType;

import java.time.LocalDate;


public record NetSalesSummaryDto(
        LocalDate netDate,
        String netProductName,
        MarginType netType,
        Long totalSalesAmount, // 판매금액 합계
        Long totalSalesCount,  // ✅ 추가: 판매수량 (이게 있어야 계산 가능!)
        Long totalReturnCount, // 반품수량 합계
        Long totalCancelPrice  // 취소금액 합계 (필요하다면)
) {}