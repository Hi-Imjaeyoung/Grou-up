package growup.spring.springserver.netsales.service;

import growup.spring.springserver.netsales.repository.NetRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}