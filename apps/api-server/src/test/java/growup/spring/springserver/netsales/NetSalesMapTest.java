package growup.spring.springserver.netsales;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class NetSalesMapTest {

    static class NetSales {}
    static class NetSalesKey {
        String name;
        String type;

        public NetSalesKey(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    public Optional<NetSales> getNetSales(Map<LocalDate, Map<NetSalesKey, NetSales>> netSalesMap,
                                          NetSalesKey key,
                                          LocalDate date) {

        return Optional.ofNullable(netSalesMap.get(date))
                .map(innerMap -> innerMap.get(key));
    }


    @Test
    @DisplayName("날짜 자체가 존재하지 않는 경우")
    void testWhenDateNotExists() {
        Map<LocalDate, Map<NetSalesKey, NetSales>> netSalesMap = new HashMap<>();

        Optional<NetSales> result =
                getNetSales(
                        netSalesMap,
                        new NetSalesKey("A", "B"),
                        LocalDate.of(2024,1,1))
                ;

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("날짜는 존재하지만, innerMap 안에 key가 없는 경우")
    void testWhenDateExistsButKeyNotExists() {
        Map<LocalDate, Map<NetSalesKey, NetSales>> netSalesMap = new HashMap<>();
        netSalesMap.put(LocalDate.of(2024,1,1), new HashMap<>());

        Optional<NetSales> result =
                getNetSales(
                        netSalesMap,
                        new NetSalesKey("A", "B"),
                        LocalDate.of(2024,1,1))
                ;

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("날짜는 존재하지만 innerMap 자체가 null 인 경우")
    void testWhenInnerMapIsNull() {
        Map<LocalDate, Map<NetSalesKey, NetSales>> netSalesMap = new HashMap<>();

        netSalesMap.put(LocalDate.of(2024,1,1), null);

        Optional<NetSales> result =
                getNetSales(
                        netSalesMap,
                        new NetSalesKey("A", "B"),
                        LocalDate.of(2024,1,1))
                ;

        assertThat(result).isEmpty();
    }
}
