package growup.spring.springserver.netsales.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@Data
@Slf4j
public class DailySalesDto {
    private LocalDate date;
    private Double cancelPrice; // 취소 매출
    private Double totalSalesPriceWithoutCancel; // 총 판매매출 (취소 매출 제외)

    @Builder
    public DailySalesDto(LocalDate date, Number cancelPrice, Number totalSalesPriceWithoutCancel) {
        this.date = date;
        this.cancelPrice = (cancelPrice != null) ? cancelPrice.doubleValue() : 0.0;
        this.totalSalesPriceWithoutCancel = (totalSalesPriceWithoutCancel != null) ? totalSalesPriceWithoutCancel.doubleValue() : 0.0;

    }
}