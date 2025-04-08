package growup.spring.springserver.netsales.repository;

import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import growup.spring.springserver.netsales.domain.NetSales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;


@DataJpaTest
class NetRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    private Member member, notmember;
    private NetSales netSales1, netSales2, netSales3, netSales4, netSales5;
    @Autowired
    private NetRepository netRepository;

    @BeforeEach
    void setup() {
        member = memberRepository.save(newMember("fa7271@naver.com"));
        notmember = memberRepository.save(newMember("windy7271@naver.com"));

        netRepository.save(newNetSales(member, "방한마스크1", MarginType.ROCKET_GROWTH, 10L, 10L, 13L, LocalDate.of(2025, 01, 01)));
        netRepository.save(newNetSales(member, "방한마스크2", MarginType.ROCKET_GROWTH, 1L, 1L, 167L, LocalDate.of(2025, 01, 02)));
        netRepository.save(newNetSales(member, "방한마스크2", MarginType.SELLER_DELIVERY, 90L, 90L, 14L, LocalDate.of(2025, 01, 03)));
        netRepository.save(newNetSales(member, "방한마스크2", MarginType.ROCKET_GROWTH, 90L, 90L, 14L, LocalDate.of(2025, 01, 03)));

        netRepository.save(newNetSales(notmember, "방방마스크1", MarginType.ROCKET_GROWTH, 80L, 80L, 1L, LocalDate.of(2025, 01, 01)));

    }

    @DisplayName("findByNetDateAndEmailAndNetProductNameAndNetMarginType : failCase.")
    @ParameterizedTest
    @MethodSource("provideFailCases_provideFailCases")
    void findByNetDateAndEmailAndNetProductNameAndNetMarginType_failCases(
            LocalDate date, String email, String productName, MarginType type) {

        Optional<NetSales> result = netRepository.findByNetDateAndEmailAndNetProductNameAndNetMarginType(
                date, email, productName, type);

        assertThat(result).isEmpty();
    }

    private static Stream<Arguments> provideFailCases_provideFailCases() {
        return Stream.of(
                Arguments.of(LocalDate.of(2025, 1, 10), "fa7271@naver.com", "방한마스크1", MarginType.ROCKET_GROWTH),   // 없는 날짜
                Arguments.of(LocalDate.of(2025, 1, 1), "wrong@naver.com", "방한마스크1", MarginType.ROCKET_GROWTH),     // 잘못된 이메일
                Arguments.of(LocalDate.of(2025, 1, 1), "fa7271@naver.com", "없는상품", MarginType.ROCKET_GROWTH),      // 잘못된 상품명
                Arguments.of(LocalDate.of(2025, 1, 1), "fa7271@naver.com", "방한마스크1", MarginType.SELLER_DELIVERY)  // 잘못된 마진타입
        );
    }

    @DisplayName("findByNetDateAndEmailAndNetProductNameAndNetMarginType : successCase.")
    @Test
    void findByNetDateAndEmailAndNetProductNameAndNetMarginType_successCase() {
        LocalDate localDate = LocalDate.of(2025, 01, 01);
        String email = "fa7271@naver.com";
        String productName = "방한마스크1";
        MarginType type = MarginType.ROCKET_GROWTH;

        NetSales netSales = netRepository.findByNetDateAndEmailAndNetProductNameAndNetMarginType(localDate, email, productName, type).get();

        assertAll(
                () -> assertThat(netSales.getNetSalesCount()).isEqualTo(10L),
                () -> assertThat(netSales.getNetReturnCount()).isEqualTo(13L),
                () -> assertThat(netSales.getNetSalesAmount()).isEqualTo(10L)
        );

    }

    @DisplayName("findDatesWithNetSalesByEmailAndDateRange : failCase.")
    @ParameterizedTest
    @MethodSource("findDatesWithNetSalesByEmailAndDateRange_provideFailCases")
    void findDatesWithNetSalesByEmailAndDateRange(
            String email, LocalDate startDate, LocalDate endDate) {

        List<LocalDate> result = netRepository.findDatesWithNetSalesByEmailAndDateRange(email, startDate, endDate);

        assertThat(result).hasSize(0);

    }

    private static Stream<Arguments> findDatesWithNetSalesByEmailAndDateRange_provideFailCases() {
        return Stream.of(
                Arguments.of("fa7271@naver.com", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 3)),   // 없는 날짜
                Arguments.of("wrong@naver.com", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3))   // 없는 이메일
        );
    }

    @DisplayName("findDatesWithNetSalesByEmailAndDateRange : successCase.")
    @Test
    void findDatesWithNetSalesByEmailAndDateRange() {
        String email = "fa7271@naver.com";
        LocalDate startDate = LocalDate.of(2025, 1, 2);
        LocalDate endDate = LocalDate.of(2025, 1, 3);

        List<LocalDate> result = netRepository.findDatesWithNetSalesByEmailAndDateRange(email, startDate, endDate);

        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result).containsExactlyInAnyOrder( // 순서 상관없이 리스트에 해당 값들이 모두 포함되어 있는지 확인
                        LocalDate.of(2025, 1, 2),
                        LocalDate.of(2025, 1, 3)
                )
        );
    }

    private Member newMember(String email) {

        return Member.builder().email(email).build();
    }

    private NetSales newNetSales(Member member, String productName, MarginType type, Long netSalesAmount, Long netSalesCount, Long netReturnCount, LocalDate netDate) {
        return NetSales.builder()
                .member(member)
                .netProductName(productName)
                .netType(type)
                .netSalesAmount(netSalesAmount)
                .netSalesCount(netSalesCount)
                .netReturnCount(netReturnCount)
                .netDate(netDate)
                .build();
    }
}