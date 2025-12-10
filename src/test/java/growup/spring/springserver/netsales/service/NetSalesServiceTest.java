package growup.spring.springserver.netsales.service;

import growup.spring.springserver.netsales.dto.DailySalesDto;
import growup.spring.springserver.netsales.dto.TotalSalesDto;
import growup.spring.springserver.netsales.repository.NetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NetSalesServiceTest {

    @InjectMocks
    private NetSalesService netSalesService;

    @Mock
    private NetRepository netRepository;

    @DisplayName("getMyTotalSales_successCase")
    @Test
    void getMyTotalSales_successCase() {

        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 1);
        String email = "fa7271@naver.com";

        List<DailySalesDto> dailySales = List.of(
                new DailySalesDto(LocalDate.of(2025, 1, 1), 100.0, 1000.0),
                new DailySalesDto(LocalDate.of(2025, 1, 1), 200.0, 2000.0),
                new DailySalesDto(LocalDate.of(2025, 1, 1), 300.0, 3000.0)
        );

        when(netRepository.findDailySalesByEmail(email, start, end)).thenReturn(dailySales);

        TotalSalesDto myTotalSales = netSalesService.getMyTotalSales(start, end, email);

        assertAll(
                () -> assertEquals(600.0, myTotalSales.getTotalCancelPrice()),
                () -> assertEquals(6000.0, myTotalSales.getTotalSalesPrice())
        );
    }
    @DisplayName("getMyTotalSales_noSalesCase")
    @Test
    void getMyTotalSales_noSalesCase() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 1);
        String email = "fa7271@naver.com";

        when(netRepository.findDailySalesByEmail(email, start, end)).thenReturn(List.of());

        TotalSalesDto myTotalSales = netSalesService.getMyTotalSales(start, end, email);

        assertAll(
                () -> assertEquals(0.0, myTotalSales.getTotalCancelPrice()),
                () -> assertEquals(0.0, myTotalSales.getTotalSalesPrice())
        );
    }
}