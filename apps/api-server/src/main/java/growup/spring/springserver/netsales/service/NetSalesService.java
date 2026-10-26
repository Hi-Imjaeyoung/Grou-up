package growup.spring.springserver.netsales.service;

import growup.spring.springserver.netsales.dto.DailySalesDto;
import growup.spring.springserver.netsales.dto.NetResultRecord;
import growup.spring.springserver.netsales.dto.NetSalesSummaryDto;
import growup.spring.springserver.netsales.dto.TotalSalesDto;
import growup.spring.springserver.netsales.repository.NetRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class NetSalesService {
    private final NetRepository netRepository;


    @Transactional
    public int deleteNetSalesReport(LocalDate date, String email) {
        return netRepository.deleteByMemberEmailAndNetDate(email, date);
    }

    public List<LocalDate> getDatesWithNetSalesByEmailAndDateRange(LocalDate start, LocalDate end, String email) {
        return netRepository.findDatesWithNetSalesByEmailAndDateRange(
                email, start, end);
    }

    public TotalSalesDto getMyTotalSales(LocalDate start, LocalDate end, String email) {

        // 날짜별 매출 리스트 조회
        List<DailySalesDto> dailySales = netRepository.findDailySalesByEmail(email, start, end);

        NetResultRecord result = getResult(dailySales);

        return TotalSalesDto.builder()
                .dailySales(dailySales)
                .totalSalesPrice(result.totalSalesPrice())
                .totalCancelPrice(result.totalCancelPrice())
                .build();
    }

    private static NetResultRecord getResult(List<DailySalesDto> dailySales) {
        double totalSalesPrice = 0.0;
        double totalCancelPrice = 0.0;

        for (DailySalesDto dailySale : dailySales) {
            totalSalesPrice += dailySale.getTotalSalesPriceWithoutCancel();
            totalCancelPrice += dailySale.getCancelPrice();
        }
        return new NetResultRecord(totalSalesPrice, totalCancelPrice);
    }
    @Cacheable(cacheNames = "salesStats", key = "#email + '_' + #start + '_' + #end")
    public List<NetSalesSummaryDto> getNetSalesByEmailAndDateRange(String email, LocalDate start, LocalDate end) {
        return netRepository.findSummaryByEmailAndDateRange(email, start, end);
    }
}