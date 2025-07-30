package growup.spring.springserver.margin.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.file.service.FileService;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import growup.spring.springserver.login.service.MemberService;
import growup.spring.springserver.margin.TypeChangeMargin;
import growup.spring.springserver.margin.converter.MarginConverter;
import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.dto.*;
import growup.spring.springserver.margin.factory.MarginConverterFactory;
import growup.spring.springserver.margin.repository.MarginRepository;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.dto.MfcDto;
import growup.spring.springserver.marginforcampaign.dto.MfcRequestWithDatesDto;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import growup.spring.springserver.netsales.domain.NetSales;
import growup.spring.springserver.netsales.repository.NetRepository;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
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
    @Mock
    private MarginConverterFactory marginConverterFactory;
    @Mock
    private FileService fileService;
    @Mock
    private NetSalesService netSalesService;

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

    //    단순히 repo에 접근해서 가져오는거라면, repoTest 에서 해준거와 중복일까요 ?
    @Test
    @DisplayName("getALLMargin_byCampaignIdAndDates")
    void getALLMargin_byCampaignIdAndDates_successCase() {
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 31);
        Long campaignId = 1L;
        Member member = getMember();

        Campaign campaign = Campaign.builder().campaignId(campaignId).member(member).camCampaignName("방한마스크").build();
        List<Margin> margins = List.of(
                Margin.builder()
                        .campaign(campaign)
                        .marDate(LocalDate.of(2024, 3, 10))
                        .marAdMargin(100L)
                        .marNetProfit(50.0)
                        .marReturnCost(10.0)
                        .build()
        );

        when(marginRepository.findByCampaignIdAndDates(campaignId, start, end)).thenReturn(margins);
        List<Margin> result = marginService.byCampaignIdAndDates(start, end, campaignId);

        assertAll(
                () -> assertThat(result.get(0).getMarAdMargin()).isEqualTo(100L),
                () -> assertThat(result).hasSize(1)
        );
    }

    @Test
    @DisplayName("getALLMargin_createNewMargin_successCase 1. 날짜가 없는 경우")
    void getALLMargin_createNewMargin_successCase() {
        LocalDate startDate = LocalDate.of(2024, 3, 1);
        LocalDate endDate = LocalDate.of(2024, 3, 8);
        Long campaignId = 1L;
        Member member = getMember();
        String email = "test@test.com";

        Campaign mockCampaign = Campaign.builder().campaignId(campaignId).member(member).camCampaignName("방한마스크").build();
        List<Margin> mockMargins = List.of(
                Margin.builder().campaign(mockCampaign).marDate(LocalDate.of(2024, 3, 2)).build(),
                Margin.builder().campaign(mockCampaign).marDate(LocalDate.of(2024, 3, 3)).build(),
                Margin.builder().campaign(mockCampaign).marDate(LocalDate.of(2024, 3, 5)).build()
        );
        List<LocalDate> mockNetSalesDates = List.of(
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 2),
                LocalDate.of(2024, 3, 3),
                LocalDate.of(2024, 3, 5),
                LocalDate.of(2024, 3, 8)
        );


        List<Margin> result = marginService.createNewMargin(mockNetSalesDates, mockMargins, mockCampaign);

        System.out.println("result = " + result);
        /*
        Todo : 2024-03-01, 2024-03-08 날짜에 대한 Margin이 생성되어야 함
         */
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
    @DisplayName("getALLMargin_createNewMargin_successCase 2. 날짜가 일치해 저장할 게 없음")
    void createNewMargin_whenAllDatesExistInMargin_thenReturnEmpty() {

        Long campaignId = 1L;
        Member member = getMember();

        Campaign campaign = Campaign.builder().campaignId(campaignId).member(member).camCampaignName("방한마스크").build();
        List<Margin> margins = List.of(
                Margin.builder().marDate(LocalDate.of(2024, 3, 1)).campaign(campaign).build(),
                Margin.builder().marDate(LocalDate.of(2024, 3, 2)).campaign(campaign).build()
        );
        List<LocalDate> netSalesDates = List.of(
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 2)
        );

        List<Margin> result = marginService.createNewMargin(netSalesDates, margins, campaign);

        verify(marginRepository, times(0)).saveAll(anyList());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getALLMargin_getUpdatableMargins_successCase 1. 업데이트 가능한 마진 조회")
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
    @DisplayName("getALLMargin_getUpdatableMargins_successCase 2. 업데이트 가능한 마진 조회 - marUpdated False, True, Null 여부 체크")
    void getALLMargin_getUpdatableMargins_successCase2() {
        List<LocalDate> mockDatesWithNetSales = List.of(
                LocalDate.of(2024, 3, 4),
                LocalDate.of(2024, 3, 5),
                LocalDate.of(2024, 3, 6),
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 2),
                LocalDate.of(2024, 3, 3)
        );

        List<Margin> mockMargins = List.of(
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 1))
                        .marUpdated(false)  // ❌ 제외됨
                        .build(),
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 2))
                        .marUpdated(true) // ✅ 업데이트 가능
                        .build(),
                Margin.builder()
                        .campaign(Campaign.builder().campaignId(1L).build())
                        .marDate(LocalDate.of(2024, 3, 3))
                        .marUpdated(null) // ✅ 업데이트 가능
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
                        m -> assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 2)), // marUpdated true
                        m -> assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 3)), // marUpdated null
                        m -> assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 4)),
                        m -> assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 5)),
                        m -> assertThat(m.getMarDate()).isEqualTo(LocalDate.of(2024, 3, 6))
                );
    }

    @Test
    @DisplayName("getALLMargin()_calculateMargin_callNetSales - 성공 케이스 1: 업데이트 완료")
    void getALLMargin_calculateMargin_callNetSales() {
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
        marginService.callNetSales(margin, campaignId, date, email);

        // Then

        assertAll(
                () -> assertThat(margin.getMarAdCost()).isEqualTo(100.0),  // 광고 비용은 초기값 그대로
                () -> assertThat(margin.getMarAdMargin()).isEqualTo((50L - 5L) * 100L + (30L - 3L) * 10L),  // 광고 마진 계산
                () -> assertThat(margin.getMarActualSales()).isEqualTo(50L + 30L - 5L - 3L),  // 실제 판매 수 계산
                () -> assertThat(margin.getMarReturnCount()).isEqualTo(5L + 3L),  // 반품 수 합산
                () -> assertThat(margin.getMarReturnCost()).isEqualTo(5L * 100L + 3L * 10L),  // 반품 비용 계산
                () -> assertThat(margin.getMarNetProfit()).isEqualTo(
                        margin.getMarAdMargin() - (margin.getMarAdCost() * 1.1) - margin.getMarReturnCost()
                )  // 순이익 계산
        );

        verify(marginService).callNetSales(margin, campaignId, date, email);
        verify(marginService, times(2))
                .checkNetSales(
                        eq(date),
                        eq(email),
                        any(String.class),
                        any(MarginType.class)
                );
    }

    @Test
    @DisplayName("getALLMargin()_calculateMargin_callNetSales - 2개 정상 집계, 1개 예외 발생 후 continue")
    void getALLMargin_calculateMargin_callNetSales_skipExceptionCase() {
        // Given
        Long campaignId = 1L;
        LocalDate date = LocalDate.of(2024, 3, 1);
        String email = "test@example.com";

        // 3개의 옵션: 첫 두 개는 정상, 세 번째는 예외

        List<MarginForCampaign> mfcs = List.of(
                MarginForCampaign.builder()
                        .mfcProductName("prodA")
                        .mfcType(MarginType.ROCKET_GROWTH)
                        .mfcPerPiece(10L)
                        .mfcReturnPrice(2L)
                        .build(),
                MarginForCampaign.builder()
                        .mfcProductName("prodB")
                        .mfcType(MarginType.SELLER_DELIVERY)
                        .mfcPerPiece(5L)
                        .mfcReturnPrice(1L)
                        .build(),
                MarginForCampaign.builder()
                        .mfcProductName("prodC")
                        .mfcType(MarginType.SELLER_DELIVERY)
                        .mfcPerPiece(3L)
                        .mfcReturnPrice(0L)
                        .build()
        );


        when(marginForCampaignRepository.MarginForCampaignByCampaignId(campaignId))
                .thenReturn(mfcs);

        NetSales ns1 = mock(NetSales.class);
        when(ns1.getNetSalesCount()).thenReturn(20L);
        when(ns1.getNetReturnCount()).thenReturn(4L);
        when(netRepository.findByNetDateAndEmailAndNetProductNameAndNetMarginType(
                date, email, "prodA", MarginType.ROCKET_GROWTH))
                .thenReturn(Optional.of(ns1));


        NetSales ns2 = mock(NetSales.class);
        when(ns2.getNetSalesCount()).thenReturn(15L);
        when(ns2.getNetReturnCount()).thenReturn(3L);
        when(netRepository.findByNetDateAndEmailAndNetProductNameAndNetMarginType(
                date, email, "prodB", MarginType.SELLER_DELIVERY))
                .thenReturn(Optional.of(ns2));

        // 세 번째 옵션은 데이터 없음 → Optional.empty() → checkNetSales()에서 예외
        when(netRepository.findByNetDateAndEmailAndNetProductNameAndNetMarginType(
                date, email, "prodC", MarginType.SELLER_DELIVERY))
                .thenReturn(Optional.empty());

        // 마진 객체 초기화 (marAdCost 반드시 0.0이 아닌 값으로 세팅)
        Margin margin = Margin.builder()
                .marAdCost(50.0)
                .build();


        // When
        marginService.callNetSales(margin, campaignId, date, email);
        // Then
        assertThat(margin.getMarActualSales()).isEqualTo(20L + 15L - 4L - 3L);  // prodA와 prodB의 실제 판매 수 합산
        assertThat(margin.getMarAdMargin()).isEqualTo(
                (20L - 4L) * 10L + (15L - 3L) * 5L);  // prodA와 prodB의 광고 마진 합산
        assertThat(margin.getMarReturnCount()).isEqualTo(4L + 3L);  // prodA와 prodB의 반품 수 합산
        assertThat(margin.getMarReturnCost()).isEqualTo(
                (4L * 2L) + (3L * 1L));  // prodA와 prodB의 반품 비용 합산
        assertThat(margin.getMarNetProfit()).isEqualTo(
                margin.getMarAdMargin() - (margin.getMarAdCost() * 1.1) - margin.getMarReturnCost());

        verify(marginService).callNetSales(margin, campaignId, date, email);
        verify(marginService, times(3))
                .checkNetSales(
                        eq(date),
                        eq(email),
                        any(String.class),
                        any(MarginType.class)
                );
    }

    @Test
    @DisplayName("getALLMargin_calculateMargin_successCase1. - 리스트 항목마다 callNetSales 호출")
    void getALLMargin_calculateMargin_successCase1() {
        // given
        Long campaignId = 42L;
        String email = "test@test.com";
        Margin m1 = Margin.builder().marDate(LocalDate.of(2024, 7, 1)).build();
        Margin m2 = Margin.builder().marDate(LocalDate.of(2024, 7, 2)).build();
        List<Margin> margins = List.of(m1, m2);

        doNothing().when(marginService)
                .callNetSales(any(Margin.class), anyLong(), any(LocalDate.class), anyString());

        marginService.calculateMargin(margins, campaignId, email);

        // 두 번 호출됐는지
        verify(marginService, times(2))
                .callNetSales(any(Margin.class), eq(campaignId), any(LocalDate.class), eq(email));

        verify(marginService).callNetSales(m1, campaignId, m1.getMarDate(), email);
        verify(marginService).callNetSales(m2, campaignId, m2.getMarDate(), email);
    }

    @Test
    @DisplayName("getALLMargin_calculateMargin_successCase2. - 빈 리스트일 때 callNetSales 미호출")
    void getALLMargin_calculateMargin_emptyList() {
        // given
        Long campaignId = 99L;
        String email = "empty@none.com";
        List<Margin> empty = List.of();

        marginService.calculateMargin(empty, campaignId, email);

        verify(marginService, never())
                .callNetSales(any(), anyLong(), any(), anyString());
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
    @DisplayName("getALLMargin() 통합테스트 successCase")
    void getALLMargin_successCase1() {
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end = LocalDate.of(2024, 3, 31);
        Long campaignId = 1L;
        Member member = getMember();
        String email = "test@test.com";

        Campaign campaign = Campaign.builder().campaignId(campaignId).member(member).camCampaignName("방한마스크").build();
        List<Margin> margins = List.of(
                Margin.builder()
                        .campaign(campaign)
                        .marDate(LocalDate.of(2024, 3, 1))
                        .marAdMargin(100L)
                        .marUpdated(false)
                        .marNetProfit(50.0)
                        .marReturnCost(10.0)
                        .marAdCost(150.0)
                        .build(),
                Margin.builder()
                        .campaign(campaign)
                        .marDate(LocalDate.of(2024, 3, 2))
                        .marUpdated(true)
                        .marAdMargin(200L)
                        .marNetProfit(100.0)
                        .marReturnCost(20.0)
                        .marAdCost(250.0)
                        .build()
        );

        List<LocalDate> netSalesDates = List.of(
                LocalDate.of(2024, 3, 2),
                LocalDate.of(2024, 3, 4)
        );
        List<Margin> createNewMargin = List.of(
                Margin.builder()
                        .campaign(campaign)
                        .marDate(LocalDate.of(2024, 3, 4))
                        .marAdMargin(300L)
                        .marNetProfit(150.0)
                        .marReturnCost(30.0)
                        .marAdCost(350.0)
                        .build()
        );
        List<Margin> updateMargin = List.of(
                Margin.builder()
                        .campaign(campaign)
                        .marDate(LocalDate.of(2024, 3, 2))
                        .marAdMargin(200L)
                        .marNetProfit(100.0)
                        .marReturnCost(20.0)
                        .marAdCost(250.0)
                        .build(),
                Margin.builder()
                        .campaign(campaign)
                        .marDate(LocalDate.of(2024, 3, 4))
                        .marAdMargin(300L)
                        .marNetProfit(150.0)
                        .marReturnCost(30.0)
                        .marAdCost(350.0)
                        .build()
        );
        List<MarginResultDto> MarginResultDtos = List.of(
                MarginResultDto.builder().marDate(LocalDate.of(2024, 3, 2)).marAdMargin(200L).marNetProfit(100.0).marReturnCost(20.0).build(),
                MarginResultDto.builder().marDate(LocalDate.of(2024, 3, 4)).marAdMargin(300L).marNetProfit(150.0).marReturnCost(30.0).build()
        );


        when(marginRepository.findByCampaignIdAndDates(campaignId, start, end))
                .thenReturn(margins);
        when(campaignService.getMyCampaign(campaignId, email))
                .thenReturn(campaign);
        when(netSalesService.getDatesWithNetSalesByEmailAndDateRange(start, end, email))
                .thenReturn(netSalesDates);
        when(marginService.createNewMargin(netSalesDates, margins, campaign))
                .thenReturn(createNewMargin);
        when(marginService.getUpdatableMargins(margins, netSalesDates, createNewMargin))
                .thenReturn(updateMargin);

        @SuppressWarnings("unchecked")
        MarginConverter<MarginResultDto> dummyConverter = mock(MarginConverter.class);
        when(marginConverterFactory.getResultConverter())
                .thenReturn(dummyConverter);

        when(dummyConverter.convert(any(Margin.class)))
                .thenAnswer(inv -> {
                    Margin m = inv.getArgument(0);
                    return MarginResultDto.builder()
                            .marDate(m.getMarDate())
                            .marAdMargin(m.getMarAdMargin())
                            .marNetProfit(m.getMarNetProfit())
                            .marReturnCost(m.getMarReturnCost())
                            .build();
                });
        when(marginService.getMarginResultDtos(margins, dummyConverter))
                .thenReturn(MarginResultDtos);

        List<MarginResponseDto> result = marginService.getALLMargin(start, end, campaignId, email);

        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .extracting(
                        MarginResponseDto::getCampaignId,
                        dto -> dto.getData().stream()
                                .map(MarginResultDto::getMarDate)
                                .collect(Collectors.toList())
                )
                .containsExactly(
                        tuple(
                                campaignId,
                                List.of(
                                        LocalDate.of(2024, 3, 2),
                                        LocalDate.of(2024, 3, 4))
                        )
                );
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

        LocalDate LatestMarginDate = LocalDate.of(2025, 11, 12);

        when(marginRepository.findLatestMarginDateByEmail(email)).thenReturn(Optional.of(LatestMarginDate));

        LocalDate result = marginService.findLatestMarginDateByEmail(email);

        assertThat(result).isEqualTo(LatestMarginDate);
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
    @DisplayName("deleteMarginsForNetSale - successCase")
    void deleteMarginsForNetSale() {
        LocalDate targetDate = LocalDate.of(2025, 11, 12);
        List<Long> campaignIds = List.of(1L, 2L);

        Margin m1 = newMargin(targetDate, Campaign.builder().campaignId(1L).build(), 100.0, 10.0);
        Margin m2 = newMargin(targetDate, Campaign.builder().campaignId(2L).build(), 200.0, 20.0);
        List<Margin> mockMargins = List.of(m1, m2);

        when(marginService.getAllMyCampaignMargin(targetDate, campaignIds)).thenReturn(mockMargins);

        int result = marginService.deleteMarginsForNetSale(targetDate, campaignIds);

        assertThat(result).isEqualTo(2);

        assertAll(
                () -> assertThat(m1.getMarReturnCost()).isZero(),
                () -> assertThat(m2.getMarReturnCost()).isZero()
        );
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
        Member member = getMember();

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

        List<MarginOverviewResponseDto> mockOverviewDtos = List.of(
                // 랭크
                new MarginOverviewResponseDto(1L, "Campaign 1", 5000.0, 500.0, 50.0, 500.0, 100.0, 1L, 50.0, 1L, 10.0),
                new MarginOverviewResponseDto(2L, "Campaign 2", 4000.0, 800.0, 40.0, 400.0, 200.0, 2L, 100.0, 2L, 15.0),
                new MarginOverviewResponseDto(3L, "Campaign 3", 3000.0, 600.0, 40.0, 300.0, 200.0, 3L, 80.0, 3L, 12.0),
                new MarginOverviewResponseDto(4L, "Campaign 4", 2000.0, 1000.0, 40.0, 500.0, 300.0, 4L, 150.0, 4L, 20.0),
                new MarginOverviewResponseDto(5L, "Campaign 5", 1000.0, 1200.0, 40.0, 600.0, 400.0, 5L, 200.0, 5L, 25.0),

                // 기타
                new MarginOverviewResponseDto(6L, "Campaign6", 250.0, 14000.0, 40.0, 700.0, 500.0, 6L, 25000.0, 6L, 30.0),
                new MarginOverviewResponseDto(7L, "Campaign8", 300.0, 14000.0, 40.0, 700.0, 500.0, 6L, 25000.0, 6L, 30.0)
        );


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