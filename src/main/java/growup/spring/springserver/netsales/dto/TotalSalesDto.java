package growup.spring.springserver.netsales.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Builder
@Slf4j
public class TotalSalesDto {
    private List<DailySalesDto> dailySales; // 날짜별 매출 리스트
    private Double totalCancelPrice;        // 기간 전체 취소금액
    private Double totalSalesPrice;         // 기간 전체 매출금액 (취소금액 안 뻄)
}