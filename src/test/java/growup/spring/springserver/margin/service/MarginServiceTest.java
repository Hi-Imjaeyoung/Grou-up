package growup.spring.springserver.margin.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.margin.TypeChangeMargin;
import growup.spring.springserver.margin.converter.MarginConverter;
import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.dto.*;
import growup.spring.springserver.margin.factory.MarginConverterFactory;
import growup.spring.springserver.margin.repository.MarginRepository;
import growup.spring.springserver.margin.util.NetSalesKey;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import growup.spring.springserver.marginforcampaignchangedbyperiod.domain.MarginForCampaignChangedByPeriod;
import growup.spring.springserver.marginforcampaignchangedbyperiod.service.MarginForCampaignChangedByPeriodService;
import growup.spring.springserver.netsales.domain.NetSales;
import growup.spring.springserver.netsales.dto.NetSalesSummaryDto;
import growup.spring.springserver.netsales.service.NetSalesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarginServiceTest {

    @Spy
    @InjectMocks
    private MarginService marginService;
    @Mock
    private CampaignService campaignService;
    @Mock
    private MarginRepository marginRepository;
    @Mock
    private MarginForCampaignRepository marginForCampaignRepository;
    @Mock
    private MarginConverterFactory marginConverterFactory;
    @Mock
    private NetSalesService netSalesService;
    @Mock
    private MarginForCampaignChangedByPeriodService marginForCampaignChangedByPeriodService;

    @Test
    @DisplayName("getCampaignAllSales(): ErrorCase1.캠패인 목록이 없을 때")
    void test1() {
        //given
        final String email = "test@test.com";
        final LocalDate date = LocalDate.of(2024, 11, 11);

        doThrow(new CampaignNotFoundException()).when(campaignService).getCampaignsByEmail(any(String.class));
        //when
        final CampaignNotFoundException result = assertThrows(CampaignNotFoundException.class,
                () -> marginService.getCampaignAllSales(email, date));
        //then
        assertThat(result.getMessage()).isEqualTo("현재 등록된 캠페인이 없습니다.");
    }

    // 날짜,
    @Test
    @DisplayName("getCampaignAllSales(): 오늘 데이터가 없는 경우 기본값 처리")
    void test2() {
        // Given
        LocalDate today = LocalDate.of(2024, 11, 11);
        LocalDate yesterday = today.minusDays(1);

        List<Campaign> campaigns = List.of(
                Campaign.builder().campaignId(1L).camCampaignName("Campaign 1").build(),
                Campaign.builder().campaignId(2L).camCampaignName("Campaign 2").build()
        );

        List<Long> campaignIds = campaigns.stream()
                .map(Campaign::getCampaignId)
                .toList();

        // 어제 데이터만 존재
        List<Margin> yesterdayMargins = List.of(
                newMargin(yesterday, campaigns.get(0), 180.0),
                newMargin(yesterday, campaigns.get(1), 240.0)
        );

        // Mock 설정
        doReturn(campaigns).when(campaignService).getCampaignsByEmail(any(String.class));
        doAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0); // 실제 호출된 값
            LocalDate from = invocation.getArgument(1);
            LocalDate to = invocation.getArgument(2);
            System.out.println("ids = " + ids);
            System.out.println("from = " + from);

            // 테스트 데이터에 `ids`가 포함된 조건 추가
            return yesterdayMargins.stream()
                    .filter(margin -> ids.contains(margin.getCampaign().getCampaignId()) &&
                            (margin.getMarDate().isEqual(from) || margin.getMarDate().isAfter(from)) &&
                            (margin.getMarDate().isEqual(to) || margin.getMarDate().isBefore(to)))
                    .toList();
        }).when(marginRepository).findByCampaignIdsAndDates(eq(campaignIds), any(LocalDate.class), any(LocalDate.class));


        // When
        List<MarginSummaryResponseDto> result = marginService.getCampaignAllSales("test@test.com", today);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTodaySales()).isEqualTo(0.0);
        assertThat(result.get(0).getYesterdaySales()).isEqualTo(180.0);
        assertThat(result.get(0).getDifferentSales()).isEqualTo(-180.0);

        assertThat(result.get(1).getTodaySales()).isEqualTo(0.0);
        assertThat(result.get(1).getYesterdaySales()).isEqualTo(240.0);
        assertThat(result.get(1).getDifferentSales()).isEqualTo(-240.0);
    }

    @Test
    @DisplayName("getCampaignAllSales(): 캠페인 1은 어제 데이터 없음, 캠페인 2는 오늘 데이터 없음")
    void test3() {
        // Given
        LocalDate today = LocalDate.of(2024, 11, 11);
        LocalDate yesterday = today.minusDays(1);

        List<Campaign> campaigns = List.of(
                Campaign.builder().campaignId(1L).camCampaignName("Campaign 1").build(),
                Campaign.builder().campaignId(2L).camCampaignName("Campaign 2").build()
        );

        List<Margin> mixedMargins = List.of(
                newMargin(today, campaigns.get(0), 200.0), // Campaign 1 오늘 매출
                newMargin(yesterday, campaigns.get(1), 240.0) // Campaign 2 어제 매출
        );

        // Mock 설정
        doReturn(campaigns).when(campaignService).getCampaignsByEmail(any(String.class));
        doReturn(mixedMargins).when(marginRepository).findByCampaignIdsAndDates(
                campaigns.stream().map(Campaign::getCampaignId).toList(),
                yesterday,
                today
        );

        // When
        List<MarginSummaryResponseDto> result = marginService.getCampaignAllSales("test@test.com", today);

        // Then
        assertThat(result).hasSize(2);

        // Campaign 1: 어제 데이터 없음
        assertThat(result.get(0).getTodaySales()).isEqualTo(200.0); // 오늘 매출
        assertThat(result.get(0).getYesterdaySales()).isEqualTo(0.0); // 어제 매출 없음
        assertThat(result.get(0).getDifferentSales()).isEqualTo(200.0); // 차이

        // Campaign 2: 오늘 데이터 없음
        assertThat(result.get(1).getTodaySales()).isEqualTo(0.0); // 오늘 매출 없음
        assertThat(result.get(1).getYesterdaySales()).isEqualTo(240.0); // 어제 매출
        assertThat(result.get(1).getDifferentSales()).isEqualTo(-240.0); // 차이
    }

    @Test
    @DisplayName("getCampaignAllSales(): Success - Dto 매핑 테스트 with getMarginForDateOrDefault")
    void test4() {
        // Given
        LocalDate today = LocalDate.of(2024, 11, 11);
        LocalDate yesterday = today.minusDays(1);

        List<Campaign> campaigns = List.of(
                Campaign.builder().campaignId(1L).camCampaignName("Campaign 1").build(),
                Campaign.builder().campaignId(2L).camCampaignName("Campaign 2").build()
        );

        List<Margin> margins = List.of(
                newMargin(today, campaigns.get(0), 200.0), // Campaign 1 오늘 매출
                newMargin(yesterday, campaigns.get(0), 180.0), // Campaign 1 어제 매출
                newMargin(today, campaigns.get(1), 300.0), // Campaign 2 오늘 매출
                newMargin(yesterday, campaigns.get(1), 240.0) // Campaign 2 어제 매출
        );

        // Mock 설정
        doReturn(campaigns).when(campaignService).getCampaignsByEmail(any(String.class));
        doReturn(margins).when(marginRepository).findByCampaignIdsAndDates(
                campaigns.stream().map(Campaign::getCampaignId).toList(),
                yesterday, today
        );

        // When
        List<MarginSummaryResponseDto> result = marginService.getCampaignAllSales("test@test.com", today);

        // Then
        assertThat(result).hasSize(2);

        MarginSummaryResponseDto campaign1 = result.get(0);
        assertThat(campaign1.getCampaignId()).isEqualTo(1L);
        assertThat(campaign1.getYesterdaySales()).isEqualTo(180.0); // 어제 매출
        assertThat(campaign1.getTodaySales()).isEqualTo(200.0); // 오늘 매출
        assertThat(campaign1.getDifferentSales()).isEqualTo(20.0); // 차이

        MarginSummaryResponseDto campaign2 = result.get(1);
        assertThat(campaign2.getCampaignId()).isEqualTo(2L);
        assertThat(campaign2.getYesterdaySales()).isEqualTo(240.0); // 어제 매출
        assertThat(campaign2.getTodaySales()).isEqualTo(300.0); // 오늘 매출
        assertThat(campaign2.getDifferentSales()).isEqualTo(60.0); // 차이
    }

    @Test
    @DisplayName("findByCampaignIdsAndDates(): Success ")
    void test5_findByCampaignIdsAndDates() {
        // Given
        LocalDate start = LocalDate.of(2024, 11, 11);
        LocalDate end = LocalDate.of(2024, 11, 30);

        List<Campaign> campaigns = List.of(
                Campaign.builder().campaignId(1L).camCampaignName("Campaign 1").build(),
                Campaign.builder().campaignId(2L).camCampaignName("Campaign 2").build()
        );
        List<Long> campaignIds = List.of(1L, 2L);

        // 7일 데이터를 생성 (DailyAdSummaryDto는 Mock된 결과로 사용)
        List<DailyAdSummaryDto> dailySummaries = List.of(
                new DailyAdSummaryDto(start, 200.0, 200.),
                new DailyAdSummaryDto(end, 180.0, 180.0)
        );

        // Mock 설정
        doReturn(campaigns).when(campaignService).getCampaignsByEmail(any(String.class));
        doReturn(dailySummaries).when(marginRepository).findMarginOverviewGraphByCampaignIdsAndDate(
                campaignIds,
                start,
                end
        );

        // When
        List<DailyAdSummaryDto> result = marginService.getMarginOverviewGraph(start, end, "test@test.com");

        // Then
        assertThat(result)
                .isNotEmpty()
                .hasSize(2);

        DailyAdSummaryDto summary1 = result.get(0);
        assertThat(summary1.getMarDate()).isEqualTo(start);
        assertThat(summary1.getMarSales()).isEqualTo(200.0);

        DailyAdSummaryDto summary2 = result.get(1);
        assertThat(summary2.getMarDate()).isEqualTo(end);
        assertThat(summary2.getMarSales()).isEqualTo(180.0);
    }

    @Test
    @DisplayName("getDailyMarginSummary : failCase 1. 캠페인이 없는 경우 ")
    void getDailyMarginSummary_failCase1() {
        //given
        doThrow(new CampaignNotFoundException()).when(campaignService).getCampaignsByEmail(any(String.class));
        String email = "test@test.com";
        LocalDate targetDate = LocalDate.of(2024, 11, 11);
        //when
        final CampaignNotFoundException result = assertThrows(CampaignNotFoundException.class,
                () -> marginService.getCampaignAllSales(email, targetDate));
        //then
        assertThat(result.getMessage()).isEqualTo("현재 등록된 캠페인이 없습니다.");

    }

    @Test
    @DisplayName("getDailyMarginSummary : failCase 2. 캠페인 리스트가 없는경우 ")
    void getDailyMarginSummary_failCase2() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 8);

        List<Campaign> campaigns = List.of(
                Campaign.builder().campaignId(1L).camCampaignName("Campaign 1").build(),
                Campaign.builder().campaignId(2L).camCampaignName("Campaign 2").build()
        );

        when(marginRepository.findByCampaignIdAndDates(eq(1L), eq(start), eq(end)))
                .thenReturn(List.of());
        when(marginRepository.findByCampaignIdAndDates(eq(2L), eq(start), eq(end)))
                .thenReturn(List.of());

        List<DailyMarginSummary> result = marginService.getDailyMarginSummary(campaigns, start, end);

        for (DailyMarginSummary dailyMarginSummary : result) {
            assertThat(dailyMarginSummary.getMarAdMargin()).isZero();
            assertThat(dailyMarginSummary.getMarNetProfit()).isZero();
        }
    }

    @Test
    @DisplayName("getDailyMarginSummary : successCase. 성공")
    void getDailyMarginSummary_successCase() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 8);

        List<Campaign> campaigns = List.of(
                Campaign.builder().campaignId(1L).camCampaignName("Campaign 1").build(),
                Campaign.builder().campaignId(2L).camCampaignName("Campaign 2").build()
        );

        // marginRepository mock 설정
        when(marginRepository.findByCampaignIdAndDates(eq(1L), eq(start), eq(end)))
                .thenReturn(List.of(newMarginDto(100L, campaigns.get(0), 100.0), newMarginDto(100L, campaigns.get(0), 100.0)));
        when(marginRepository.findByCampaignIdAndDates(eq(2L), eq(start), eq(end)))
                .thenReturn(List.of(newMarginDto(100L, campaigns.get(1), 100.0), newMarginDto(100L, campaigns.get(0), 100.0)));


        // when
        List<DailyMarginSummary> result = marginService.getDailyMarginSummary(campaigns, start, end);

        // then
        assertThat(result).hasSize(2);
        for (DailyMarginSummary dailyMarginSummary : result) {
            assertThat(dailyMarginSummary.getMarAdMargin()).isEqualTo(200);
            assertThat(dailyMarginSummary.getMarNetProfit()).isEqualTo(200);
        }

    }


    @Test
    @DisplayName("getALLMargin_createNewMargin_successCase 1. 새로운 날짜로 마진 생성 (3/1, 3/8)")
    void getALLMargin_createNewMargin_successCase() {
        Long campaignId = 1L;
        Member member = getMember();
        String email = "test@test.com";

        Campaign myCampaign = Campaign.builder().campaignId(campaignId).member(member).camCampaignName("방한마스크").build();

        List<LocalDate> datesWithNetSales = List.of(
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 2),
                LocalDate.of(2024, 3, 3),
                LocalDate.of(2024, 3, 5),
                LocalDate.of(2024, 3, 8)
        );

        when(marginRepository.findExistingDatesByCampaignIdAndDateIn(campaignId, datesWithNetSales))
                .thenReturn(Set.of(
                        LocalDate.of(2024, 3, 2),
                        LocalDate.of(2024, 3, 3),
                        LocalDate.of(2024, 3, 5)
                ));

        List<Margin> result = marginService.createNewMargin(datesWithNetSales, myCampaign);

        assertThat(result)
                .isNotNull()
                .hasSize(2)
                .satisfiesExactlyInAnyOrder(
                        m -> {
                            assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 1));
                            assertThat(m.getCampaign().getCampaignId()).isEqualTo(campaignId);
                            assertThat(m.getCampaign().getMember().getEmail()).isEqualTo(email);
                            assertThat(m.getMarReturnCost()).isEqualTo(0.0);
                        },
                        m -> {
                            assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 8));
                            assertThat(m.getCampaign().getCampaignId()).isEqualTo(campaignId);
                            assertThat(m.getCampaign().getMember().getEmail()).isEqualTo(email);
                            assertThat(m.getMarReturnCost()).isEqualTo(0.0);
                        }
                );


        verify(marginRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("getMargin_createNewMargin_successCase 2. 날짜가 일치해 저장할 게 없음")
    void createNewMargin_whenAllDatesExistInMargin_thenReturnEmpty() {

        Long campaignId = 1L;
        Member member = getMember();

        Campaign myCampaign = Campaign.builder().campaignId(campaignId).member(member).camCampaignName("방한마스크").build();

        List<LocalDate> datesWithNetSales = List.of(
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 2)
        );

        when(marginRepository.findExistingDatesByCampaignIdAndDateIn(campaignId, datesWithNetSales))
                .thenReturn(Set.of(
                        LocalDate.of(2024, 3, 1),
                        LocalDate.of(2024, 3, 2),
                        LocalDate.of(2024, 3, 5)
                ));

        List<Margin> result = marginService.createNewMargin(datesWithNetSales, myCampaign);
        verify(marginRepository, times(0)).saveAll(anyList());
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("getMargin_netSalesMap_SuccessCase. 순매출 맵 생성 성공")
    void getMargin_netSalesMap_SuccessCase() {
        String email = "test@test.com";
        LocalDate startDate = LocalDate.of(2024, 3, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 2);


        NetSales ns1 = createNetSales(1L, "방한마스크", MarginType.ROCKET_GROWTH, 1000L, 10L, 0L, 0L, LocalDate.of(2024, 3, 1));
        NetSales ns2 = createNetSales(2L, "여름마스크", MarginType.ROCKET_GROWTH, 1000L, 10L, 0L, 0L, LocalDate.of(2024, 3, 2));
        NetSalesSummaryDto summary1 = createNetSalesSummaryDto(ns1);

        NetSalesSummaryDto summary2 = createNetSalesSummaryDto(ns2);

        when(netSalesService.getNetSalesByEmailAndDateRange(email, startDate, endDate))
                .thenReturn(List.of(summary1, summary2));

        Map<LocalDate, Map<NetSalesKey, NetSalesSummaryDto>> result = marginService.getNetSalesMap(startDate, endDate, email);
        // then
        // 1. 사이즈가 쳌
        assertThat(result).isNotNull().hasSize(2);

        // 2. [3월 1일] 데이터 검증 - groupingBy가 날짜별로 잘 묶었는지
        Map<NetSalesKey, NetSalesSummaryDto> mapDay1 = result.get(startDate);
        assertThat(mapDay1).isNotNull();

        // 3. [3월 1일]
        NetSalesKey key1 = new NetSalesKey("방한마스크", MarginType.ROCKET_GROWTH);
        assertThat(mapDay1).containsKey(key1); // 키가 제대로 생성되었는지
        assertThat(mapDay1.get(key1)).isEqualTo(summary1); // 값이 제대로 들어있는지

        // 4. [3월 2일]
        Map<NetSalesKey, NetSalesSummaryDto> mapDay2 = result.get(endDate);
        NetSalesKey key2 = new NetSalesKey("여름마스크", MarginType.ROCKET_GROWTH);

        assertThat(mapDay2).containsKey(key2);
        assertThat(mapDay2.get(key2)).isEqualTo(summary2);
    }

    @Test
    @DisplayName("getMargin_getUpdatableMargins_successCase 1. 업데이트 가능한 마진 조회")
    void getALLMargin_getUpdatableMargins_successCase() {
        List<LocalDate> mockDatesWithNetSales = List.of(
                LocalDate.of(2024, 3, 4),
                LocalDate.of(2024, 3, 5),
                LocalDate.of(2024, 3, 6)
        );

        List<Margin> mockMargins = List.of(
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 1)).build(),
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 2)).build(),
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 3))
                        .build()
        );

        List<Margin> mockCreateNewMargin = List.of(
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 4)).build(),
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 5)).build(),
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 6)).build()
        );

        List<Margin> result = marginService.getUpdatableMargins(mockMargins, mockDatesWithNetSales, mockCreateNewMargin);

        assertThat(result)
                .isNotNull()
                .hasSize(3)
                .satisfiesExactlyInAnyOrder(
                        m -> assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 4)),
                        m -> assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 5)),
                        m -> assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 6))
                );
    }

    @Test
    @DisplayName("getMargin_getUpdatableMargins_successCase 2. 업데이트 가능한 마진 조회")
    void getALLMargin_getUpdatableMargins_successCase2() {
        List<LocalDate> mockDatesWithNetSales = List.of(
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 2),
                LocalDate.of(2024, 3, 4),
                LocalDate.of(2024, 3, 5),
                LocalDate.of(2024, 3, 6)
        );

        List<Margin> mockMargins = List.of(
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 1)).build(),
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 2)).build(),
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 3))
                        .build()
        );

        List<Margin> mockCreateNewMargin = List.of(
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 4)).build(),
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 5)).build(),
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 6)).build()
        );

        List<Margin> result = marginService.getUpdatableMargins(mockMargins, mockDatesWithNetSales, mockCreateNewMargin);

        assertThat(result)
                .isNotNull()
                .hasSize(5)
                .satisfiesExactlyInAnyOrder(
                        m -> assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 1)),
                        m -> assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 2)),
                        m -> assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 4)),
                        m -> assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 5)),
                        m -> assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 6))
                );
    }

    @Test
    @DisplayName("getMargin_getMarginChangesByCampaignAndDateRange_successCase. 마진 변경내역 맵 생성 성공")
    void getALLMargin_getMarginChangesByCampaignAndDateRange_successCase() {
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 15);
        Long campaignId = 1L;
        Member member = getMember();
        Campaign campaign = Campaign.builder().campaignId(campaignId).member(member).camCampaignName("방한마스크").build();


        List<MarginForCampaignChangedByPeriod> marginForCampaignChangedByPeriods = List.of(
                creteMarginForCampaignChangedByPeriod(newMarginForCampaign(1L, campaign, "방한마스크 빨강색", MarginType.ROCKET_GROWTH, 100L, 100L), 1L, LocalDate.of(2024, 3, 1), 50L, 50L, 5L, 5L),
                creteMarginForCampaignChangedByPeriod(newMarginForCampaign(2L, campaign, "방한마스크 빨강색", MarginType.SELLER_DELIVERY, 33L, 33L), 2L, LocalDate.of(2024, 3, 2), 20L, 30L, 5L, 5L),
                creteMarginForCampaignChangedByPeriod(newMarginForCampaign(3L, campaign, "방한마스크 파랑색", MarginType.ROCKET_GROWTH, 13L, 21L), 3L, LocalDate.of(2024, 3, 2), 20L, 30L, 5L, 5L)
        );

        when(marginForCampaignChangedByPeriodService.findAllByMfcCbpIdsAndDateRange(
                anyList(),
                eq(start),
                eq(end)
        )).thenReturn(Map.of(
                LocalDate.of(2024, 3, 1),
                Map.ofEntries(Map.entry(1L, marginForCampaignChangedByPeriods.get(0))),
                LocalDate.of(2024, 3, 2),
                Map.ofEntries(
                        Map.entry(2L, marginForCampaignChangedByPeriods.get(1)),
                        Map.entry(3L, marginForCampaignChangedByPeriods.get(2))
                )
        ));

        Map<LocalDate, Map<Long, MarginForCampaignChangedByPeriod>> result =
                marginService.marginChangesByDate(start, end, campaignId);

        assertThat(result).hasSize(2);
        assertThat(result.get(LocalDate.of(2024, 3, 1))).containsKey(1L);
        assertThat(result.get(LocalDate.of(2024, 3, 2))).containsKey(2L);
    }

    /*
       마진은 3/1, 3/2 , 3/12 날짜에 존재
       netSales 는 3/1, 3/2 일에 존재
       수정내역은 3/1 일에는 전부 다 존재, 3/2 일에는 일부만 수정
    */
    @Test
    @DisplayName("getMargin_calculateMarginForCampaign_successCase. 마진 계산 성공")
    void getALLMargin_calculateMarginForCampaign_successCase() {
        Long campaignId = 1L;
        Member member = getMember();

        Campaign campaign = Campaign.builder().campaignId(campaignId).member(member).camCampaignName("방한마스크").build();

        List<Margin> margins = List.of(
                Margin.builder()
                        .id(1L)
                        .campaign(campaign)
                        .marDate(LocalDate.of(2024, 3, 1))
                        .marAdMargin(100L)
                        .marNetProfit(50.0)
                        .marReturnCost(10.0)
                        .marAdCost(150.0)
                        .build(),
                Margin.builder()
                        .id(2L)
                        .marDate(LocalDate.of(2024, 3, 2))
                        .marAdMargin(200L)
                        .marNetProfit(100.0)
                        .marReturnCost(20.0)
                        .marAdCost(250.0)
                        .build(),
                Margin.builder()
                        .id(3L)
                        .marDate(LocalDate.of(2024, 3, 12))
                        .marAdMargin(200L)
                        .marNetProfit(100.0)
                        .marReturnCost(20.0)
                        .marAdCost(250.0)
                        .build()
        );
        // 기본 값
        MarginForCampaign marginForCampaign1 = newMarginForCampaign(1L, campaign, "방한마스크 빨강색", MarginType.ROCKET_GROWTH, 100L, 100L);
        MarginForCampaign marginForCampaign2 = newMarginForCampaign(2L, campaign, "방한마스크 빨강색", MarginType.SELLER_DELIVERY, 33L, 33L);
        MarginForCampaign marginForCampaign3 = newMarginForCampaign(3L, campaign, "방한마스크 파랑색", MarginType.ROCKET_GROWTH, 13L, 21L);
        MarginForCampaign marginForCampaign4 = newMarginForCampaign(4L, campaign, "방한마스크 기본값", MarginType.ROCKET_GROWTH, 7L, 7L);
        List<MarginForCampaign> marginForCampaigns = List.of(
                marginForCampaign1,
                marginForCampaign2,
                marginForCampaign3,
                marginForCampaign4
        );
        // 날짜별 변경값이 있을경우
        MarginForCampaignChangedByPeriod mcpCbp1 = creteMarginForCampaignChangedByPeriod(marginForCampaign1, 1L, LocalDate.of(2024, 3, 1), 50L, 50L, 5L, 5L);
        MarginForCampaignChangedByPeriod mcpCbp2 = creteMarginForCampaignChangedByPeriod(marginForCampaign2, 2L, LocalDate.of(2024, 3, 2), 20L, 30L, 5L, 5L);
        MarginForCampaignChangedByPeriod mcpCbp3 = creteMarginForCampaignChangedByPeriod(marginForCampaign3, 3L, LocalDate.of(2024, 3, 2), 20L, 30L, 5L, 5L);
        Map<LocalDate, Map<Long, MarginForCampaignChangedByPeriod>> getMarginChangesByCampaignAndDateRange =
                Map.of(
                        LocalDate.of(2024, 3, 1),
                        Map.ofEntries(
                                Map.entry(mcpCbp1.getMarginForCampaign().getId(), mcpCbp1),
                                Map.entry(mcpCbp2.getMarginForCampaign().getId(), mcpCbp2)),

                        LocalDate.of(2024, 3, 2),
                        Map.ofEntries(
                                Map.entry(mcpCbp1.getMarginForCampaign().getId(), mcpCbp1),
                                Map.entry(mcpCbp3.getMarginForCampaign().getId(), mcpCbp3)
                        ));

        // 3/1: 빨강 10개 판매 (반품 0), 파랑 5개 판매 (반품 0)
        NetSales ns1 = createNetSales(1L, "방한마스크 빨강색", MarginType.ROCKET_GROWTH, 1000L, 10L, 0L, 0L, LocalDate.of(2024, 3, 1));
        NetSales ns2 = createNetSales(2L, "방한마스크 파랑색", MarginType.ROCKET_GROWTH, 1000L, 5L, 0L, 0L, LocalDate.of(2024, 3, 1));

        // 3/2: 빨강 20개 판매 (반품 2), 없는색, 기본값 7개 판매 (반품 7),
        // 로켓 빨강색은 변경값, 없는색 계산 x, 기본값은 기본값,
        NetSales ns3 = createNetSales(3L, "방한마스크 빨강색", MarginType.ROCKET_GROWTH, 2000L, 20L, 2L, 0L, LocalDate.of(2024, 3, 2));
        NetSales ns4 = createNetSales(4L, "방한마스크 없는색", MarginType.ROCKET_GROWTH, 2000L, 20L, 2L, 0L, LocalDate.of(2024, 3, 2));
        NetSales ns5 = createNetSales(5L, "방한마스크 기본값", MarginType.ROCKET_GROWTH, 2000L, 7L, 7L, 0L, LocalDate.of(2024, 3, 2));


        Map<LocalDate, Map<NetSalesKey, NetSalesSummaryDto>> netSalesMap =
                Map.of(
                        LocalDate.of(2024, 3, 1),
                        Map.of(
                                createNetSalesKey("방한마스크 빨강색", MarginType.ROCKET_GROWTH),
                                createNetSalesSummaryDto(ns1),
                                createNetSalesKey("방한마스크 파랑색", MarginType.ROCKET_GROWTH),
                                createNetSalesSummaryDto(ns2)
                        ),
                        LocalDate.of(2024, 3, 2),
                        Map.of(
                                createNetSalesKey("방한마스크 빨강색", MarginType.ROCKET_GROWTH),
                                createNetSalesSummaryDto(ns3),
                                createNetSalesKey("방한마스크 없는색", MarginType.ROCKET_GROWTH),
                                createNetSalesSummaryDto(ns4),
                                createNetSalesKey("방한마스크 기본값", MarginType.ROCKET_GROWTH),
                                createNetSalesSummaryDto(ns5)
                        ));

        when(marginForCampaignRepository.MarginForCampaignByCampaignId(campaignId))
                .thenReturn(marginForCampaigns);


        marginService.calculateMargin(
                margins,
                campaignId,
                getMarginChangesByCampaignAndDateRange,
                netSalesMap
        );
        assertThat(margins)
                .extracting(
                        Margin::getMarActualSales,
                        Margin::getMarAdMargin,
                        Margin::getMarReturnCount,
                        Margin::getMarReturnCost
                )
                .containsExactly(
                        tuple(15L, 605L, 0L, 0.0),   //  (3/1)
                        tuple(18L, 900L, 9L, 59.0),  //  (3/2)
                        tuple(0L, 0L, 0L, 0.0)      // (3/12)
                );
    }

    @Test
    @DisplayName("getALLMargin_getMarginResultDtos_successCase1. - MarginResultDto 변환 성공")
    void getALLMargin_getMarginResultDtos() {
        @SuppressWarnings("unchecked")
        MarginConverter<MarginResultDto> dummyConverter = mock(MarginConverter.class);

        when(dummyConverter.convert(any(Margin.class)))
                .thenAnswer(invocation -> {
                    Margin m = invocation.getArgument(0);
                    return MarginResultDto.builder()
                            .marDate(m.getMarDate())
                            .marAdMargin(m.getMarAdMargin())
                            .marNetProfit(m.getMarNetProfit())
                            .marReturnCost(m.getMarReturnCost())
                            .build();
                });

        List<Margin> margin = List.of(
                Margin.builder()
                        .marDate(LocalDate.of(2024, 7, 1))
                        .marAdMargin(100L)
                        .marNetProfit(50.0)
                        .marReturnCost(10.0)
                        .build(),
                Margin.builder()
                        .marDate(LocalDate.of(2024, 7, 2))
                        .marAdMargin(100L)
                        .marNetProfit(50.0)
                        .marReturnCost(10.0)
                        .build(),
                Margin.builder()
                        .marDate(LocalDate.of(2024, 7, 7))
                        .marAdMargin(200L)
                        .marNetProfit(100.0)
                        .marReturnCost(20.0)
                        .build()
        );

        List<MarginResultDto> result = marginService.getMarginResultDtos(margin, dummyConverter);

        assertThat(result)
                .isNotNull()
                .hasSize(3)
                .extracting(
                        MarginResultDto::getMarDate,
                        MarginResultDto::getMarAdMargin,
                        MarginResultDto::getMarNetProfit,
                        MarginResultDto::getMarReturnCost
                )
                .containsExactly(
                        tuple(LocalDate.of(2024, 7, 1), 100L, 50.0, 10.0),
                        tuple(LocalDate.of(2024, 7, 2), 100L, 50.0, 10.0),
                        tuple(LocalDate.of(2024, 7, 7), 200L, 100.0, 20.0)
                );
    }

    @Test
    @DisplayName("getALLMargin_getMarginResultDtos_successCase2. - MarginResultDto 변환 실패")
    void getALLMargin_getMarginResultDtos_failCase() {
        @SuppressWarnings("unchecked")
        MarginConverter<MarginResultDto> dummyConverter = mock(MarginConverter.class);

        when(dummyConverter.convert(any(Margin.class)))
                .thenThrow(new RuntimeException());

        List<Margin> margin = List.of(
                Margin.builder()
                        .marDate(LocalDate.of(2024, 7, 1))
                        .marAdMargin(100L)
                        .marNetProfit(50.0)
                        .marReturnCost(10.0)
                        .build()
        );

        assertThrows(RuntimeException.class, () -> marginService.getMarginResultDtos(margin, dummyConverter));
    }

    @Test
    @DisplayName("getALLMargin_createMarginResponseDto_successCase1. - MarginResponseDto 변환 성공")
    void getALLMargin_createMarginResponseDto_successCase1() {
        Long campaignId = 123L;
        List<MarginResultDto> dtos = List.of(
                MarginResultDto.builder().marDate(LocalDate.of(2024, 7, 1)).marAdMargin(10L).marNetProfit(5.0).marReturnCost(1.0).build(),
                MarginResultDto.builder().marDate(LocalDate.of(2024, 7, 2)).marAdMargin(20L).marNetProfit(10.0).marReturnCost(2.0).build()
        );

        MarginResponseDto response = TypeChangeMargin.createMarginResponseDto(campaignId, dtos);

        assertThat(response).isNotNull();
        assertThat(response.getCampaignId()).isEqualTo(campaignId);
        assertThat(response.getData()).hasSize(2);
    }

    @Test
    @DisplayName("getALLMargin_createMarginResponseDto_successCase2. - 빈 리스트일 때")
    void getALLMargin_createMarginResponseDto_successCase2() {
        Long campaignId = 456L;
        List<MarginResultDto> dtos = List.of();

        MarginResponseDto response = TypeChangeMargin.createMarginResponseDto(campaignId, dtos);

        assertThat(response).isNotNull();
        assertThat(response.getCampaignId()).isEqualTo(campaignId);
        assertThat(response.getData()).isEmpty();
    }

    @Test
    @DisplayName("getALLMargin_successCase: 기존 마진 업데이트 및 신규 마진 생성 흐름 검증")
    void getALLMargin_successCase() {
        // given
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 2);
        Long campaignId = 1L;
        String email = "test@test.com";

        Campaign campaign = Campaign.builder()
                .campaignId(campaignId)
                .camCampaignName("방한마스크")
                .member(Member.builder().email(email).build())
                .build();

        Margin existingMargin = Margin.builder()
                .marDate(LocalDate.of(2024, 3, 1))
                .campaign(campaign)
                .marAdMargin(0L)
                .marAdCost(0.0)
                .marReturnCount(0L)
                .build();

        //3/1, 3/2 둘 다 판매 있음
        NetSales ns1 = createNetSales(1L, "방한마스크", MarginType.ROCKET_GROWTH, 1000L, 10L, 0L, 0L, LocalDate.of(2024, 3, 1));
        NetSales ns2 = createNetSales(2L, "방한마스크", MarginType.ROCKET_GROWTH, 1000L, 10L, 0L, 0L, LocalDate.of(2024, 3, 2));
        List<NetSalesSummaryDto> netSalesList = List.of(createNetSalesSummaryDto(ns1), createNetSalesSummaryDto(ns2));
        List<LocalDate> datesWithSales = List.of(
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 2));

        MarginForCampaign mfc = newMarginForCampaign(1L, campaign, "방한마스크", MarginType.ROCKET_GROWTH, 100L, 100L); // 개당 마진 100원

        when(campaignService.getMyCampaign(campaignId, email)).thenReturn(campaign);

        when(marginRepository.findByCampaignIdAndDates(campaignId, start, end))
                .thenReturn(new ArrayList<>(List.of(existingMargin)));
        when(netSalesService.getDatesWithNetSalesByEmailAndDateRange(start, end, email))
                .thenReturn(datesWithSales);
        when(marginRepository.findExistingDatesByCampaignIdAndDateIn(campaignId, datesWithSales))
                .thenReturn(Set.of(LocalDate.of(2024, 3, 1)));
        when(marginForCampaignChangedByPeriodService.findAllByMfcCbpIdsAndDateRange(any(), eq(start), eq(end)))
                .thenReturn(Collections.emptyMap());
        when(netSalesService.getNetSalesByEmailAndDateRange(email, start, end))
                .thenReturn(netSalesList);
        when(marginForCampaignRepository.MarginForCampaignByCampaignId(campaignId))
                .thenReturn(List.of(mfc));

        @SuppressWarnings("unchecked")
        MarginConverter<MarginResultDto> mockConverter = mock(MarginConverter.class);
        when(marginConverterFactory.getResultConverter()).thenReturn(mockConverter);

        when(mockConverter.convert(any(Margin.class))).thenAnswer(invocation -> {
            Margin m = invocation.getArgument(0);
            return MarginResultDto.builder()
                    .marDate(m.getMarDate())
                    .marAdMargin(m.getMarAdMargin())
                    .build();
        });

        // when
        List<MarginResponseDto> result = marginService.getALLMargin(start, end, campaignId, email);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getData())
                .extracting(MarginResultDto::getMarDate, MarginResultDto::getMarAdMargin)
                .contains(
                        tuple(LocalDate.of(2024, 3, 1), 1000L)
                );

    }


    @DisplayName("createMarginTable_createMarginTable failCase 1. 이미 있는 CampaignId")
    @Test
    void createMarginTable_failCase1() {

        // Given
        LocalDate targetDate = LocalDate.of(2025, 11, 11);
        Long campaignId = 1L;
        Member member = getMember();
        String email = "test@test.com";


        Campaign campaign = Campaign.builder().campaignId(campaignId).member(member).camCampaignName("방한마스크").build();
        Margin margin = Margin.builder()
                .id(20L)
                .campaign(campaign)
                .marDate(targetDate)
                .marAdMargin(0L)
                .marNetProfit(0.0)
                .marAdCost(0.0).build();

        when(campaignService.getMyCampaign(campaignId, email)).thenReturn(campaign);
        when(marginRepository.findByCampaignIdAndDate(campaignId, targetDate)).thenReturn(Optional.of(margin));

        // When
        Long result = marginService.createMarginTable(targetDate, campaignId, email);
        assertThat(result).isEqualTo(20L);
    }

    @Test
    @DisplayName("createMarginTable_successCase1: CampaignNotFoundException 처리")
    void createMarginTable_failCase_CampaignNotFound() {
        // Given
        LocalDate targetDate = LocalDate.of(2025, 11, 12);
        Long campaignId = 1L;
        String email = "test@test.com";

        when(campaignService.getMyCampaign(campaignId, email)).thenThrow(new CampaignNotFoundException());

        // When
        CampaignNotFoundException exception = assertThrows(CampaignNotFoundException.class,
                () -> marginService.createMarginTable(targetDate, campaignId, email));

        // Then
        assertThat(exception.getMessage()).isEqualTo("현재 등록된 캠페인이 없습니다.");
        verify(campaignService).getMyCampaign(campaignId, email);
        verify(marginRepository, never()).save(any(Margin.class)); // 저장을 호출하지 않음
    }

    @Test
    @DisplayName("createMarginTable_successCase2: 새로운 Margin 생성 성공")
    void createMarginTable_successCase2() {
        // Given
        LocalDate targetDate = LocalDate.of(2025, 11, 12);
        Long campaignId = 1L;
        Member member = getMember();
        String email = "test@test.com";

        Campaign campaign = Campaign.builder().campaignId(campaignId).member(member).camCampaignName("방한마스크").build();

        when(campaignService.getMyCampaign(campaignId, email)).thenReturn(campaign);
        when(marginRepository.findByCampaignIdAndDate(campaignId, targetDate)).thenReturn(Optional.empty());

        Margin mockSavedMargin = Margin.builder()
                .id(100L) // 테스트용 임의의 ID
                .campaign(campaign)
                .marDate(targetDate)
                .build();

        when(marginRepository.save(any(Margin.class))).thenReturn(mockSavedMargin);

        // When
        Long result = marginService.createMarginTable(targetDate, campaignId, email);

        // Then
        assertThat(result).isEqualTo(100L);

        // Verify
        verify(marginRepository).save(any(Margin.class));
        verify(campaignService).getMyCampaign(campaignId, email);
    }

    @Test
    @DisplayName("findLatestMarginDateByEmail_successCase ")
    void findLatestMarginDateByEmail_successCase() {
        String email = "test@test.com";

        LocalDate latestMarginDate = LocalDate.of(2025, 11, 12);

        when(marginRepository.findLatestMarginDateByEmail(email)).thenReturn(Optional.of(latestMarginDate));

        LocalDate result = marginService.findLatestMarginDateByEmail(email);

        assertThat(result).isEqualTo(latestMarginDate);
        verify(marginRepository).findLatestMarginDateByEmail(email);

    }

    @Test
    @DisplayName("findLatestMarginDateByEmail_failCase ")
    void findLatestMarginDateByEmail_failCase() {
        String email = "noData@test.com";

        when(marginRepository.findLatestMarginDateByEmail(email)).thenReturn(Optional.empty());

        LocalDate result = marginService.findLatestMarginDateByEmail(email);

        assertThat(result).isEqualTo(LocalDate.now());

    }

    @Test
    @DisplayName("getMarginOverview() - 캠페인이 5개 이하일 때 그대로 반환한다")
    void getMarginOverview_Success_WhenSizeIsLessThanOrEqualTo5() {
        // given: 테스트 기본 데이터 설정
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 31);
        String userEmail = "test@test.com";

        List<Campaign> mockCampaigns = List.of(
                Campaign.builder().campaignId(1L).build(),
                Campaign.builder().campaignId(2L).build(),
                Campaign.builder().campaignId(3L).build()
        );
        List<Long> campaignIds = List.of(1L, 2L, 3L);

        List<MarginOverviewResponseDto> mockOverviewDtos = List.of(
                new MarginOverviewResponseDto(1L, "Campaign 1", 1000.0, 500.0, 50.0, 500.0, 100.0, 1L, 50.0, 1L, 10.0),
                new MarginOverviewResponseDto(2L, "Campaign 2", 2000.0, 800.0, 40.0, 400.0, 200.0, 2L, 100.0, 2L, 15.0),
                new MarginOverviewResponseDto(3L, "Campaign 3", 1500.0, 600.0, 40.0, 300.0, 200.0, 3L, 80.0, 3L, 12.0)
        );

        when(campaignService.getCampaignsByEmail(userEmail)).thenReturn(mockCampaigns);
        when(marginRepository.findMarginOverviewByCampaignIdsAndDate(start, end, campaignIds))
                .thenReturn(mockOverviewDtos);

        List<MarginOverviewResponseDto> marginOverview = marginService.getMarginOverview(start, end, userEmail);

        assertThat(marginOverview)
                .isNotNull()
                .hasSize(3);
        assertThat(marginOverview)
                .extracting(
                        MarginOverviewResponseDto::getCampaignId,
                        MarginOverviewResponseDto::getCampaignName,
                        MarginOverviewResponseDto::getMarSales
                )
                .containsExactlyInAnyOrder(
                        tuple(1L, "Campaign 1", 1000.0),
                        tuple(2L, "Campaign 2", 2000.0),
                        tuple(3L, "Campaign 3", 1500.0)
                );
    }

    @Test
    @DisplayName("getMarginOverview().createOthersSummary() - 캠페인 5개 이상일때 나머지 합치기")
    void getMarginOverView_ETC() {
        // given: 테스트 기본 데이터 설정
        List<MarginOverviewResponseDto> etcDto = List.of(
                new MarginOverviewResponseDto(6L, "기타 캠페인1", 5000.0, 2000.0, 100.0, 1000.0, 500.0, 6L, 300.0, 1L, 50.0),
                new MarginOverviewResponseDto(11L, "기타 캠페인2", 5000.0, 2000.0, 100.0, 1000.0, 500.0, 6L, 300.0, 1L, 50.0),
                new MarginOverviewResponseDto(7L, "기타 캠페인3", 5000.0, 2000.0, 100.0, 1000.0, 500.0, 6L, 300.0, 1L, 50.0),
                new MarginOverviewResponseDto(8L, "기타 캠페인4", 5000.0, 2000.0, 100.0, 1000.0, 500.0, 6L, 300.0, 1L, 50.0),
                new MarginOverviewResponseDto(9L, "기타 캠페인5", 5000.0, 2000.0, 100.0, 1000.0, 500.0, 6L, 300.0, 1L, 50.0)
        );
        MarginOverviewResponseDto result = TypeChangeMargin.createOthersSummary(etcDto);


        assertThat(result)
                .isNotNull()
                .extracting(
                        MarginOverviewResponseDto::getCampaignId,
                        MarginOverviewResponseDto::getCampaignName,
                        MarginOverviewResponseDto::getMarSales,
                        MarginOverviewResponseDto::getMarNetProfit,
                        MarginOverviewResponseDto::getMarMarginRate, // (getMarNetProfit/getMarSales) * 100
                        MarginOverviewResponseDto::getMarRoi, // 순이익 / 집행광고비 * 100
                        MarginOverviewResponseDto::getMarAdCost,
                        MarginOverviewResponseDto::getMarReturnCount,
                        MarginOverviewResponseDto::getMarReturnCost,
                        MarginOverviewResponseDto::getMarAdConversionSalesCount,
                        MarginOverviewResponseDto::getMarReturnRate // // 반품률: (반품갯수/광고전환주문수) * 100
                )
                .containsExactly(
                        0L, "기타", 25000.0, 10000.0, 40.0, 400.0, 2500.0, 30L, 1500.0, 5L, 600.0
                );
    }

    @Test
    @DisplayName("getMarginOverview - 통합 테스트")
    void getMarginOverview() {
        // given: 테스트 기본 데이터 설정
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 31);
        String userEmail = "test@test.com";

        List<Campaign> mockCampaigns = List.of(
                Campaign.builder().member(getMember()).campaignId(1L).build(),
                Campaign.builder().member(getMember()).campaignId(2L).build(),
                Campaign.builder().member(getMember()).campaignId(3L).build(),
                Campaign.builder().member(getMember()).campaignId(4L).build(),
                Campaign.builder().member(getMember()).campaignId(5L).build(),
                Campaign.builder().member(getMember()).campaignId(6L).build(),
                Campaign.builder().member(getMember()).campaignId(7L).build()
        );

        List<Long> ids = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L);

        List<MarginOverviewResponseDto> mockOverviewDtos = new ArrayList<>(List.of(
                // 랭크
                new MarginOverviewResponseDto(1L, "Campaign 1", 5000.0, 500.0, 50.0, 500.0, 100.0, 1L, 50.0, 1L, 10.0),
                new MarginOverviewResponseDto(2L, "Campaign 2", 4000.0, 800.0, 40.0, 400.0, 200.0, 2L, 100.0, 2L, 15.0),
                new MarginOverviewResponseDto(3L, "Campaign 3", 3000.0, 600.0, 40.0, 300.0, 200.0, 3L, 80.0, 3L, 12.0),
                new MarginOverviewResponseDto(4L, "Campaign 4", 2000.0, 1000.0, 40.0, 500.0, 300.0, 4L, 150.0, 4L, 20.0),
                new MarginOverviewResponseDto(5L, "Campaign 5", 1000.0, 1200.0, 40.0, 600.0, 400.0, 5L, 200.0, 5L, 25.0),

                // 기타
                new MarginOverviewResponseDto(6L, "Campaign6", 250.0, 14000.0, 40.0, 700.0, 500.0, 6L, 25000.0, 6L, 30.0),
                new MarginOverviewResponseDto(7L, "Campaign8", 300.0, 14000.0, 40.0, 700.0, 500.0, 6L, 25000.0, 6L, 30.0)
        ));


        when(campaignService.getCampaignsByEmail(userEmail)).thenReturn(mockCampaigns);
        when(marginRepository.findMarginOverviewByCampaignIdsAndDate(start, end, ids))
                .thenReturn(mockOverviewDtos);

        List<MarginOverviewResponseDto> marginOverview = marginService.getMarginOverview(start, end, userEmail);

        assertThat(marginOverview)
                .isNotNull()
                .hasSize(6)
                .extracting(MarginOverviewResponseDto::getCampaignId)
                .containsExactly(1L, 2L, 3L, 4L, 5L, 0L);

        MarginOverviewResponseDto othersDto = marginOverview.get(5);

        assertAll(
                () -> assertThat(othersDto.getCampaignName()).isEqualTo("기타"),
                () -> assertThat(othersDto.getMarSales()).isEqualTo(550.0),
                () -> assertThat(othersDto.getCampaignId()).isZero()
        );
    }

    private Margin newMargin(LocalDate date, Campaign campaign, Double marsale) {
        return Margin.builder()
                .marDate(date)
                .campaign(campaign)
                .marSales(marsale)
                .build();
    }

    private Margin newMargin(LocalDate date, Campaign campaign, Double marsale, Double marReturnCost) {
        return Margin.builder()
                .marDate(date)
                .campaign(campaign)
                .marSales(marsale)
                .marReturnCost(marReturnCost)
                .marAdMargin(100L)
                .marActualSales(100L)
                .marAdCost(100.0)
                .build();
    }

    private Margin newMarginDto(Long marAdMargin, Campaign campaign, Double marNetProfit) {
        return Margin.builder()
                .marAdMargin(marAdMargin)
                .campaign(campaign)
                .marNetProfit(marNetProfit)
                .build();
    }

    private MarginForCampaign newMarginForCampaign(Long id, Campaign campaign, String productName, MarginType mfcType, Long mfcReturnPrice, Long mfcPerPiece) {
        return MarginForCampaign.builder()
                .id(id)
                .campaign(campaign)
                .mfcProductName(productName)
                .mfcType(mfcType)
                .mfcReturnPrice(mfcReturnPrice)
                .mfcPerPiece(mfcPerPiece)
                .build();
    }

    public Member getMember() {
        return Member.builder()
                .email("test@test.com")
                .build();
    }

    private MarginForCampaignChangedByPeriod creteMarginForCampaignChangedByPeriod(
            MarginForCampaign marginForCampaign, Long id, LocalDate date, Long salePrice, Long totalPrice, Long costPrice, Long returnPrice) {
        return MarginForCampaignChangedByPeriod.builder()
                .id(id)
                .date(date)
                .salePrice(salePrice)
                .totalPrice(totalPrice)
                .costPrice(costPrice)
                .returnPrice(returnPrice)
                .marginForCampaign(marginForCampaign)
                .build();

    }

    private NetSalesKey createNetSalesKey(String productName, MarginType marginType) {
        return new NetSalesKey(productName, marginType);
    }

    private NetSales createNetSales(Long id, String netProductName, MarginType netType, Long netSalesAmount,
                                    Long netSalesCount, Long netReturnCount, Long netCancelPrice, LocalDate netDate) {
        return NetSales.builder()
                .id(id)
                .netProductName(netProductName)
                .netType(netType)
                .netSalesAmount(netSalesAmount)
                .netSalesCount(netSalesCount)
                .netReturnCount(netReturnCount)
                .netCancelPrice(netCancelPrice)
                .netDate(netDate)
                .build();
    }

    private NetSalesSummaryDto createNetSalesSummaryDto(NetSales ns) {
        return new NetSalesSummaryDto(
                ns.getNetDate(),
                ns.getNetProductName(),
                ns.getNetType(),
                ns.getNetSalesAmount(),
                ns.getNetSalesCount(),
                ns.getNetReturnCount(),
                ns.getNetCancelPrice()
        );
    }

}
