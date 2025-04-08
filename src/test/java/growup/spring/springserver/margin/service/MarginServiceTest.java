package growup.spring.springserver.margin.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.exception.netsales.NetSalesNotFoundProductName;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import growup.spring.springserver.login.service.MemberService;
import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.dto.*;
import growup.spring.springserver.margin.repository.MarginRepository;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.dto.MfcDto;
import growup.spring.springserver.marginforcampaign.dto.MfcRequestWithDatesDto;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import growup.spring.springserver.netsales.domain.NetSales;
import growup.spring.springserver.netsales.repository.NetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
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
    private MemberService memberService;
    @Mock
    private MarginRepository marginRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private NetRepository netRepository;
    @Mock
    private MarginForCampaignRepository marginForCampaignRepository;

    @Test
    @DisplayName("getCampaignAllSales(): ErrorCase1.캠패인 목록이 없을 때")
    void test1() {
        //given
        doThrow(new CampaignNotFoundException()).when(campaignService).getCampaignsByEmail(any(String.class));
        //when
        final CampaignNotFoundException result = assertThrows(CampaignNotFoundException.class,
                () -> marginService.getCampaignAllSales("test@test.com", LocalDate.of(2024, 11, 11)));
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
    @DisplayName("findByCampaignIdsAndDates(): Success - 7일간 데이터 요약")
    void test5_findByCampaignIdsAndDates() {
        // Given
        LocalDate today = LocalDate.of(2024, 11, 11);
        LocalDate sevenDaysAgo = today.minusDays(7);

        List<Campaign> campaigns = List.of(
                Campaign.builder().campaignId(1L).camCampaignName("Campaign 1").build(),
                Campaign.builder().campaignId(2L).camCampaignName("Campaign 2").build()
        );

        // 7일 데이터를 생성 (DailyAdSummaryDto는 Mock된 결과로 사용)
        List<DailyAdSummaryDto> dailySummaries = List.of(
                new DailyAdSummaryDto(today, 200.0, 200.0, 1.0), // 오늘 데이터
                new DailyAdSummaryDto(sevenDaysAgo, 180.0, 180.0, 1.0) // 7일 전 데이터
        );

        // Mock 설정
        doReturn(campaigns).when(campaignService).getCampaignsByEmail(any(String.class));
        doReturn(dailySummaries).when(marginRepository).find7daysTotalsByCampaignIds(
                campaigns.stream().map(Campaign::getCampaignId).toList(),
                sevenDaysAgo,
                today
        );

        // When
        List<DailyAdSummaryDto> result = marginService.findByCampaignIdsAndDates("test@test.com", today);

        // Then
        assertThat(result)
                .isNotEmpty()
                .hasSize(2);

        DailyAdSummaryDto summary1 = result.get(0);
        assertThat(summary1.getMarDate()).isEqualTo(today);
        assertThat(summary1.getMarAdCost()).isEqualTo(200.0);
        assertThat(summary1.getMarSales()).isEqualTo(200.0);
        assertThat(summary1.getMarRoas()).isEqualTo(1.0);

        DailyAdSummaryDto summary2 = result.get(1);
        assertThat(summary2.getMarDate()).isEqualTo(sevenDaysAgo);
        assertThat(summary2.getMarAdCost()).isEqualTo(180.0);
        assertThat(summary2.getMarSales()).isEqualTo(180.0);
        assertThat(summary2.getMarRoas()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("getDailyMarginSummary : failCase 1. 캠페인이 없는 경우 ")
    void getDailyMarginSummary_failCase1() {
        //given
        doThrow(new CampaignNotFoundException()).when(campaignService).getCampaignsByEmail(any(String.class));
        //when
        final CampaignNotFoundException result = assertThrows(CampaignNotFoundException.class,
                () -> marginService.getCampaignAllSales("test@test.com", LocalDate.of(2024, 11, 11)));
        //then
        assertThat(result.getMessage()).isEqualTo("현재 등록된 캠페인이 없습니다.");

    }

    @Test
    @DisplayName("getDailyMarginSummary : failCase 2. 캠페인 리스트가 없는경우 ")
    void getDailyMarginSummary_failCase2() {
        String email = "test@naver.com";
        LocalDate targetDate = LocalDate.of(2025, 1, 1);

        List<Campaign> campaigns = List.of(
                Campaign.builder().campaignId(1L).camCampaignName("Campaign 1").build(),
                Campaign.builder().campaignId(2L).camCampaignName("Campaign 2").build()
        );

        // Mock 설정
        doReturn(campaigns).when(campaignService).getCampaignsByEmail(any(String.class));

        when(marginRepository.findByCampaignIdAndDate(eq(1L), eq(targetDate)))
                .thenReturn(Optional.empty());
        when(marginRepository.findByCampaignIdAndDate(eq(2L), eq(targetDate)))
                .thenReturn(Optional.empty());

        List<DailyMarginSummary> result = marginService.getDailyMarginSummary(email, targetDate);

        assertThat(result).isEmpty();

    }

    @Test
    @DisplayName("getDailyMarginSummary : successCase. 성공")
    void getDailyMarginSummary_successCase() {
        String email = "test@naver.com";
        LocalDate targetDate = LocalDate.of(2025, 1, 1);

        List<Campaign> campaigns = List.of(
                Campaign.builder().campaignId(1L).camCampaignName("Campaign 1").build(),
                Campaign.builder().campaignId(2L).camCampaignName("Campaign 2").build()
        );
        doReturn(campaigns).when(campaignService).getCampaignsByEmail(any(String.class));

        // marginRepository mock 설정
        when(marginRepository.findByCampaignIdAndDate(eq(1L), eq(targetDate)))
                .thenReturn(Optional.of(newMarginDto(100L, campaigns.get(0), 100.0)));
        when(marginRepository.findByCampaignIdAndDate(eq(2L), eq(targetDate)))
                .thenReturn(Optional.of(newMarginDto(100L, campaigns.get(1), 100.0)));

        List<DailyMarginSummary> expected = List.of(
                DailyMarginSummary.builder()
                        .marProductName("Campaign 1")
                        .marAdMargin(100L)
                        .marNetProfit(100.0)
                        .build(),
                DailyMarginSummary.builder()
                        .marProductName("Campaign 2")
                        .marAdMargin(100L)
                        .marNetProfit(100.0)
                        .build()
        );

        // when
        List<DailyMarginSummary> result = marginService.getDailyMarginSummary(email, targetDate);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);  // 순서에 상관없이 일치하는지 체크


    }

    @Test
    @DisplayName("updateEfficiencyAndAdBudget : 일부 실패 케이스")
    void updateEfficiencyAndAdBudget_failCase() {
        // given
        Long campaignId = 1L;

        MarginUpdateRequestDto dto1 = MarginUpdateRequestDto.builder()
                .id(1L)
                .marDate(LocalDate.of(2024, 3, 25))
                .marTargetEfficiency(120.0)
                .marAdBudget(10000.0)
                .build();

        MarginUpdateRequestDto dto2 = MarginUpdateRequestDto.builder()
                .id(999L)
                .marDate(LocalDate.of(2024, 3, 25))
                .marTargetEfficiency(110.0)
                .marAdBudget(15000.0)
                .build();

        MarginUpdateRequestDtos requestDtos = MarginUpdateRequestDtos.builder()
                .campaignId(campaignId)
                .data(List.of(dto1, dto2))
                .build();

        // Mock 세팅: id=1L 은 성공, id=999L은 실패
        when(marginRepository.findById(1L)).thenReturn(Optional.of(createMargin(1L)));
        when(marginRepository.findById(999L)).thenReturn(Optional.empty());

        // when
        MarginUpdateResponseDto result = marginService.updateEfficiencyAndAdBudget(requestDtos);

        // then
        assertThat(result.getRequestNumber()).isEqualTo(2);
        assertThat(result.getResponseNumber()).isEqualTo(1);
        assertThat(result.getFailedDate()).hasSize(1);

        // 실패한 날짜에 대해 제대로 실패 데이터가 들어갔는지 확인
        Map<String, Double> failEntry = result.getFailedDate().get(LocalDate.of(2024, 3, 25));

        assertThat(failEntry)
                .containsEntry("targetEfficiency", 110.0)
                .containsEntry("adBudget", 15000.0);
    }

    @Test
    @DisplayName("updateEfficiencyAndAdBudget : 전부 성공")
    void updateEfficiencyAndAdBudget_allSuccess() {
        // given
        Long campaignId = 1L;

        MarginUpdateRequestDto dto1 = MarginUpdateRequestDto.builder()
                .id(1L)
                .marDate(LocalDate.of(2024, 3, 25))
                .marTargetEfficiency(120.0)
                .marAdBudget(10000.0)
                .build();

        MarginUpdateRequestDto dto2 = MarginUpdateRequestDto.builder()
                .id(2L)
                .marDate(LocalDate.of(2024, 3, 26))
                .marTargetEfficiency(110.0)
                .marAdBudget(15000.0)
                .build();

        MarginUpdateRequestDtos requestDtos = MarginUpdateRequestDtos.builder()
                .campaignId(campaignId)
                .data(List.of(dto1, dto2))
                .build();

        // Mock 세팅: 두 id 모두 성공
        when(marginRepository.findById(1L)).thenReturn(Optional.of(createMargin(1L)));
        when(marginRepository.findById(2L)).thenReturn(Optional.of(createMargin(2L)));

        // when
        MarginUpdateResponseDto result = marginService.updateEfficiencyAndAdBudget(requestDtos);

        // then
        assertThat(result.getRequestNumber()).isEqualTo(2);
        assertThat(result.getResponseNumber()).isEqualTo(2);
        assertThat(result.getFailedDate()).isEmpty();

    }

    @DisplayName("getALLMargin() - failCase 1 : NetSales 데이터가 없는 경우")
    @Test
    void getALLMargin_failCase2() {
        LocalDate start = LocalDate.of(2024, 11, 8);
        LocalDate end = LocalDate.of(2024, 11, 10);
        String email = "test@test.com";
        Member member = getMember();
        Long campaignId = 1L;

        Campaign campaign = Campaign.builder().campaignId(campaignId).member(member).build();

        doReturn(List.of()) // 기존 마진 없음
                .when(marginRepository)
                .findByCampaignIdAndDates(campaignId, start, end);

        doReturn(campaign)
                .when(campaignService)
                .getMyCampaign(any(Long.class), any(String.class));

        doReturn(List.of()) // NetSales 없음
                .when(netRepository)
                .findDatesWithNetSalesByEmailAndDateRange(email, start, end);

        List<MarginResponseDto> result = marginService.getALLMargin(start, end, campaignId, email);
        assertThat(result.get(0).getData()).isEmpty();
    }

    @DisplayName("getALLMargin() - 실패 케이스 2: MarginForCampaign 데이터 없음")
    @Test
    void getALLMargin_failCase_noMarginForCampaign() {
        LocalDate start = LocalDate.of(2024, 11, 8);
        LocalDate end = LocalDate.of(2024, 11, 10);
        String email = "test@test.com";
        Long campaignId = 1L;
        Member member = getMember();

        Campaign campaign = Campaign.builder().campaignId(campaignId).member(member).build();
        Margin margin = Margin.builder()
                .campaign(campaign)
                .marDate(start)
                .marAdMargin(0L)
                .marNetProfit(0.0)
                .marAdCost(0.0).build();

        doReturn(List.of(margin))
                .when(marginRepository)
                .findByCampaignIdAndDates(campaignId, start, end);

        doReturn(campaign)
                .when(campaignService)
                .getMyCampaign(any(Long.class), any(String.class));

        doReturn(List.of(start))
                .when(netRepository)
                .findDatesWithNetSalesByEmailAndDateRange(email, start, end);

        doReturn(List.of()) // 옵션 데이터 없음
                .when(marginForCampaignRepository)
                .MarginForCampaignByCampaignId(campaignId);

        List<MarginResponseDto> result = marginService.getALLMargin(start, end, campaignId, email);

        assertThat(result.get(0).getData())
                .allMatch(m -> m.getMarAdMargin() == 0 && m.getMarNetProfit() == 0.0);
    }

    @DisplayName("getALLMargin() - failCase 3 : 기존 마진도 없고 NetSales도 없으면 빈 리스트 반환, ")
    @Test
    void getALLMargin_failCase3() {
        // given
        LocalDate start = LocalDate.of(2024, 11, 8);
        LocalDate end = LocalDate.of(2024, 11, 10);
        String email = "test@test.com";
        Member member = getMember();
        Long campaignId = 1L;
        Campaign campaign = Campaign.builder().campaignId(campaignId).member(member).build();

        doReturn(List.of()) // 기존 마진 없음
                .when(marginRepository).findByCampaignIdAndDates(campaignId, start, end);

        doReturn(campaign)
                .when(campaignService)
                .getMyCampaign(campaignId, email);

        doReturn(List.of()) // NetSales 없음
                .when(netRepository)
                .findDatesWithNetSalesByEmailAndDateRange(email, start, end);

        // when
        List<MarginResponseDto> result = marginService.getALLMargin(start, end, campaignId, email);

        // then
        Set<LocalDate> existingDates = result.stream()
                .flatMap(dto -> dto.getData().stream())
                .map(MarginResultDto::getMarDate)  // Margin 객체에서 날짜를 가져옵니다.
                .collect(Collectors.toSet());

        List<LocalDate> newMarginDates = result.stream()
                .flatMap(dto -> dto.getData().stream())
                .map(MarginResultDto::getMarDate)
                .collect(Collectors.toList());

        // Verify
        assertAll(
                () -> assertThat(result.get(0).getCampaignId()).isEqualTo(1L),
                () -> assertThat(result.get(0).getData()).isEmpty(),
                () -> assertThat(existingDates).isEmpty(),
                () -> assertThat(newMarginDates).isEmpty()
        );
    }

    @Test
    @DisplayName("getALLMargin()_checkNetSales- 실패 케이스 4: NetSalesNotFoundProductName 예외 발생 시")
    void checkNetSales_failCase_4() {

        doReturn(Optional.empty())
                .when(netRepository)
                .findByNetDateAndEmailAndNetProductNameAndNetMarginType(any(LocalDate.class), any(String.class), any(String.class), eq(MarginType.ROCKET_GROWTH));

        final NetSalesNotFoundProductName result = assertThrows(NetSalesNotFoundProductName.class,
                () -> marginService.checkNetSales(LocalDate.now(), "fa7271@naver.com", "방한 마스크", MarginType.ROCKET_GROWTH));
        //then
        assertThat(result.getMessage()).isEqualTo("없는 상품아이디 입니다.");
    }

    @Test
    @DisplayName("getALLMargin()_callNetSales - 성공 케이스 1: 업데이트 완료")
    void getALLMargin_callNetSales() {
        // Given
        Long campaignId = 1L;
        LocalDate date = LocalDate.now();
        String email = "test@example.com";

        // MarginForCampaign mock 데이터
        List<MarginForCampaign> marginForCampaigns = List.of(
                MarginForCampaign.builder()
                        .id(1L)
                        .mfcProductName("보석1")
                        .mfcType(MarginType.ROCKET_GROWTH)
                        .mfcPerPiece(100L)
                        .mfcReturnPrice(100L)
                        .build(),
                MarginForCampaign.builder()
                        .id(2L)
                        .mfcProductName("보석2")
                        .mfcType(MarginType.SELLER_DELIVERY)
                        .mfcPerPiece(10L)
                        .mfcReturnPrice(10L)
                        .build()
        );

        // marginForCampaignRepository mock
        doReturn(marginForCampaigns)
                .when(marginForCampaignRepository)
                .MarginForCampaignByCampaignId(campaignId);

        // 첫 번째 NetSales mock 설정
        NetSales netSales1 = mock(NetSales.class);
        when(netSales1.getNetSalesCount()).thenReturn(50L);  // 실제 판매 수
        when(netSales1.getNetReturnCount()).thenReturn(5L);  // 반품 수

        // 두 번째 NetSales mock 설정
        NetSales netSales2 = mock(NetSales.class);
        when(netSales2.getNetSalesCount()).thenReturn(30L);  // 실제 판매 수
        when(netSales2.getNetReturnCount()).thenReturn(3L);  // 반품 수

        // checkNetSales 메서드가 NetSales를 반환하도록 mock
        doReturn(Optional.of(netSales1))
                .when(netRepository)
                .findByNetDateAndEmailAndNetProductNameAndNetMarginType(date, email, "보석1", MarginType.ROCKET_GROWTH);

        doReturn(Optional.of(netSales2))
                .when(netRepository)
                .findByNetDateAndEmailAndNetProductNameAndNetMarginType(date, email, "보석2", MarginType.SELLER_DELIVERY);

        // Margin 객체 생성
        Margin margin = Margin.builder()
                .marAdMargin(0L)
                .marNetProfit(0.0)
                .marSales(0.0)
                .marReturnCount(0L)
                .marReturnCost(0.0)
                .marAdCost(100.0)
                .build();

        // When
        Margin updatedMargin = marginService.callNetSales(margin, campaignId, date, email);

        // Then
        assertThat(updatedMargin.getMarActualSales()).isEqualTo((50L + 30L));  // 실제 판매 수 합산 (50 + 30)
        assertThat(updatedMargin.getMarAdMargin()).isEqualTo((50L * 100L) + (30L * 10L));  // 광고 마진 합산
        assertThat(updatedMargin.getMarReturnCount()).isEqualTo((5L + 3L));  // 반품 수 합산 (5 + 3)
        assertThat(updatedMargin.getMarReturnCost()).isEqualTo((5L * 100L) + (3L * 10L));  // 반품 비용 합산
        assertThat(updatedMargin.getMarNetProfit()).isEqualTo(
                updatedMargin.getMarAdMargin() - (updatedMargin.getMarAdCost() * 1.1) - updatedMargin.getMarReturnCost());
    }

    @Test
    @DisplayName("getALLMargin_calculateMargin_success1. 첫 번째 margin만 callNetSales 호출, 두 번째 margin은 호출 안 되는지 검증")
    void getALLMargin_calculateMargin_case1() {
        // given
        Margin firstMargin = Margin.builder()
                .marAdMargin(0L)
                .marNetProfit(0.0)
                .marReturnCost(0.0)
                .marDate(LocalDate.now())
                .build();

        Margin secondMargin = Margin.builder()
                .marAdMargin(100L)
                .marNetProfit(50.0)
                .marReturnCost(10.0)
                .marDate(LocalDate.now().minusDays(1))
                .build();

        List<Margin> margins = List.of(firstMargin, secondMargin);
        Long campaignId = 1L;
        String email = "test@email.com";

        Margin updatedMargin = Margin.builder()
                .marAdMargin(500L)
                .marNetProfit(300.0)
                .marReturnCost(50.0)
                .marDate(LocalDate.now())
                .build();

        doReturn(updatedMargin)
                .when(marginService)
                .callNetSales(any(Margin.class), eq(campaignId), any(LocalDate.class), eq(email));

        // when
        marginService.calculateMargin(margins, campaignId, email);

        // then
        ArgumentCaptor<Margin> marginCaptor = ArgumentCaptor.forClass(Margin.class);
        verify(marginService, times(1))
                .callNetSales(marginCaptor.capture(), eq(campaignId), any(LocalDate.class), eq(email));

        Margin capturedMargin = marginCaptor.getValue();
        // 첫 번째 margin 객체와 동일한 객체가 넘어갔는지 검증
        assertThat(capturedMargin).isEqualTo(firstMargin);

        // 두 번째 margin은 호출되지 않았으므로 verify never 사용
        verify(marginService, never())
                .callNetSales(eq(secondMargin), eq(campaignId), any(LocalDate.class), eq(email));
    }

    @Test
    @DisplayName("getALLMargin_calculateMargin_success2. - 성공 케이스: 마진 업데이트")
    void getALLMargin_calculateMargin_case2() {
        List<Margin> margins = List.of(
                Margin.builder()
                        .marAdMargin(0L)
                        .marNetProfit(0.0)
                        .marReturnCost(0.0)
                        .marDate(LocalDate.now())
                        .build(),
                Margin.builder()
                        .marAdMargin(100L)
                        .marNetProfit(50.0)
                        .marReturnCost(10.0)
                        .marDate(LocalDate.now().minusDays(1))
                        .build()
        );

        Long campaignId = 1L;
        String email = "test@email.com";

        // callNetSales가 호출될 때 반환할 객체 세팅
        Margin updatedMargin = Margin.builder()
                .marAdMargin(500L)
                .marNetProfit(300.0)
                .marReturnCost(50.0)
                .marDate(LocalDate.now())
                .build();

        // 이 부분이 작동하려면 marginService가 @Spy 여야 함
        doReturn(updatedMargin)
                .when(marginService)
                .callNetSales(any(Margin.class), eq(campaignId), any(LocalDate.class), eq(email));

        // when
        List<Margin> result = marginService.calculateMargin(margins, campaignId, email);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMarAdMargin()).isEqualTo(500L);
        assertThat(result.get(1).getMarAdMargin()).isEqualTo(100L);

        verify(marginService, times(1)).callNetSales(any(), eq(campaignId), any(), eq(email));
    }

    @Test
    @DisplayName("getALLMargin - 성공 케이스: 기존 + 신규 margin 생성 및 계산 후 DTO 반환")
    void getALLMargin_success() {
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 31);
        Long campaignId = 1L;
        Member member = getMember();
        String email = "test@test.com";

        // 1. 기존 Margin mock
        List<Margin> existingMargins = List.of(
                Margin.builder()
                        .marAdMargin(100L)
                        .marNetProfit(50.0)
                        .marReturnCost(10.0)
                        .marDate(LocalDate.of(2024, 3, 10))
                        .build()
        );

        // 2. 캠페인 mock
        Campaign myCampaign = Campaign.builder()
                .campaignId(campaignId)
                .camCampaignName("테스트 캠페인")
                .member(member)
                .build();

        // 3. NetSales에 존재하는 날짜 (기존 margin 날짜 외)
        List<LocalDate> netSalesDates = List.of(
                LocalDate.of(2024, 3, 10), // 기존에 이미 있는 날짜
                LocalDate.of(2024, 3, 15)  // 새로운 날짜
        );

        // 4. 새로 생성될 Margin (newMargin)
        Margin newMargin = Margin.builder()
                .marAdMargin(0L)
                .marNetProfit(0.0)
                .marReturnCost(0.0)
                .marDate(LocalDate.of(2024, 3, 15))
                .build();

        // 5. 계산된 최종 Margin mock (기존 + 업데이트)
        List<Margin> calculatedMargins = List.of(
                existingMargins.get(0),
                newMargin
        );


        // when stub 세팅
        when(marginRepository.findByCampaignIdAndDates(campaignId, start, end)).thenReturn(existingMargins);
        when(campaignService.getMyCampaign(campaignId, email)).thenReturn(myCampaign);
        when(netRepository.findDatesWithNetSalesByEmailAndDateRange(email, start, end)).thenReturn(netSalesDates);
        when(marginRepository.saveAll(anyList())).thenReturn(List.of(newMargin));
        doReturn(calculatedMargins).when(marginService).calculateMargin(anyList(), eq(campaignId), eq(email));

        // 실행
        List<MarginResponseDto> result = marginService.getALLMargin(start, end, campaignId, email);

        // then 검증

        assertAll(
                () -> {
                    MarginResponseDto dto = result.get(0);
                    List<MarginResultDto> data = dto.getData();

                    assertThat(dto.getCampaignId()).isEqualTo(campaignId);
                    assertThat(data).hasSize(2);

                    // 첫 번째 Margin 검증
                    assertThat(data.get(0).getMarDate()).isEqualTo(LocalDate.of(2024, 3, 10));
                    assertThat(data.get(0).getMarReturnCost()).isEqualTo(10.0);

                    // 두 번째 Margin 검증
                    assertThat(data.get(1).getMarDate()).isEqualTo(LocalDate.of(2024, 3, 15));
                    assertThat(data.get(1).getMarReturnCost()).isEqualTo(0.0);
                }
        );


        // verify
        verify(marginRepository).findByCampaignIdAndDates(campaignId, start, end);
        verify(campaignService).getMyCampaign(campaignId, email);
        verify(netRepository).findDatesWithNetSalesByEmailAndDateRange(email, start, end);
        verify(marginRepository).saveAll(anyList());
        verify(marginService).calculateMargin(anyList(), eq(campaignId), eq(email));

        // any - 모든값 허용, 느슨 eq - 특정값 허용,엄격 정확
    }

    @Test
    @DisplayName("marginUpdatesByPeriod : 업데이트 성공")
    void marginUpdatesByPeriod() {
        LocalDate start = LocalDate.of(2025, 3, 1);
        LocalDate end = LocalDate.of(2025, 3, 1);
        Long campaignId = 1L;
        Member member = getMember();
        String email = "test@test.com";
        Campaign campaign = Campaign.builder().campaignId(campaignId).member(member).camCampaignName("방한마스크").build();

        MfcRequestWithDatesDto request = MfcRequestWithDatesDto.builder()
                .startDate(LocalDate.of(2024, 4, 1))
                .endDate(LocalDate.of(2024, 4, 10))
                .campaignId(1L)
                .data(List.of(
                        MfcDto.builder()
                                .mfcProductName("방한마스크 빨강색")
                                .mfcType(MarginType.SELLER_DELIVERY)
                                .mfcPerPiece(500L)  // 변경된 값
                                .mfcReturnPrice(100L)
                                .build(),
                        MfcDto.builder()
                                .mfcProductName("방한마스크 파랑색")
                                .mfcType(MarginType.ROCKET_GROWTH)
                                .mfcPerPiece(600L)  // 변경된 값
                                .mfcReturnPrice(150L)
                                .build()
                ))
                .build();


        List<Margin> margins = List.of(
                newMargin(LocalDate.of(2025, 03, 01), campaign, 100.0, 10.0)
        );


        List<MarginForCampaign> marginForCampaigns = List.of(
                newMarginForCampaign(campaign, "방한마스크 빨강색", MarginType.ROCKET_GROWTH, 100L, 100L),
                newMarginForCampaign(campaign, "방한마스크 빨강색", MarginType.SELLER_DELIVERY, 33L, 33L),
                newMarginForCampaign(campaign, "방한마스크 파랑색", MarginType.ROCKET_GROWTH, 13L, 21L)
        );

        // 첫 번째 NetSales mock 설정
        NetSales netSales1 = mock(NetSales.class);
        when(netSales1.getNetSalesCount()).thenReturn(50L);  // 실제 판매 수
        when(netSales1.getNetReturnCount()).thenReturn(5L);  // 반품 수

        // 두 번째 NetSales mock 설정
        NetSales netSales2 = mock(NetSales.class);
        when(netSales2.getNetSalesCount()).thenReturn(30L);  // 실제 판매 수
        when(netSales2.getNetReturnCount()).thenReturn(3L);  // 반품 수

        NetSales netSales3 = mock(NetSales.class);
        when(netSales3.getNetSalesCount()).thenReturn(25L);  // 실제 판매 수
        when(netSales3.getNetReturnCount()).thenReturn(5L);  // 반품 수


        when(netRepository.findByNetDateAndEmailAndNetProductNameAndNetMarginType(
                any(LocalDate.class),
                anyString(),
                eq("방한마스크 빨강색"),
                eq(MarginType.ROCKET_GROWTH)
        )).thenReturn(Optional.of(netSales1));

        when(netRepository.findByNetDateAndEmailAndNetProductNameAndNetMarginType(
                any(LocalDate.class),
                anyString(),
                eq("방한마스크 빨강색"),
                eq(MarginType.SELLER_DELIVERY)
        )).thenReturn(Optional.of(netSales2));

        when(netRepository.findByNetDateAndEmailAndNetProductNameAndNetMarginType(
                any(LocalDate.class),
                anyString(),
                eq("방한마스크 파랑색"),
                eq(MarginType.ROCKET_GROWTH)
        )).thenReturn(Optional.of(netSales3));

        when(marginRepository.findByCampaignIdAndDates(
                anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(margins);

        when(marginForCampaignRepository.MarginForCampaignByCampaignId(campaignId)).thenReturn(marginForCampaigns);

        marginService.marginUpdatesByPeriod(request, email);

        Margin updatedMargin = margins.get(0);  // 변경된 Margin 객체 가져오기

//        { "방한마스크 빨강색 + MarginType.ROCKET_GROWTH } 는 값이 변경되지 않기때문에 기존에 MarginForCampaign에 저장된 값으로 진행

        long expectedAdMargin =
                (50 * 100) + (30 * 500) + (25 * 600);
        long expectedReturnPrice =
                (5 * 100) + (3 * 100) + (5 * 150);

        assertEquals(expectedAdMargin, updatedMargin.getMarAdMargin());
        assertEquals(expectedReturnPrice, updatedMargin.getMarReturnCost());
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

    private MarginForCampaign newMarginForCampaign(Campaign campaign, String productName, MarginType mfcType, Long mfcReturnPrice, Long mfcPerPiece) {
        return MarginForCampaign.builder()
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

    private Margin createMargin(Long id) {
        return Margin.builder()
                .id(id)
                .marAdBudget(0.0)
                .marTargetEfficiency(0.0)
                .build();
    }

}