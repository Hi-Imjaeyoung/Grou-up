package growup.spring.springserver.campaignoptiondetails.service;

import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.campaignoptiondetails.domain.CampaignOptionDetails;
import growup.spring.springserver.campaignoptiondetails.dto.CampaignOptionDetailsResponseDto;
import growup.spring.springserver.campaignoptiondetails.repository.CampaignOptionDetailsRepository;
import growup.spring.springserver.exception.global.InvalidDateFormatException;
import growup.spring.springserver.execution.domain.Execution;
import growup.spring.springserver.execution.repository.ExecutionRepository;
import growup.spring.springserver.execution.service.ExecutionService;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class CampaignOptionDetailsServiceTest {

    @Mock
    private CampaignOptionDetailsRepository campaignOptionDetailsRepository;

    @Mock
    private ExecutionService executionService;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private CampaignOptionDetailsService campaignOptionDetailsService;

    @Test
    @DisplayName("getCampaignDetails : Error 1. 해당 켐페인의 옵션이 없는경우")
    void test1() {
        // Given
        doReturn(List.of())
                .when(executionService)
                .findExecutionIdsByCampaignId(any());

        // When & Then
        List<CampaignOptionDetailsResponseDto> campaignDetailsByCampaignsIds = campaignOptionDetailsService.getCampaignDetailsByCampaignsIds(
                LocalDate.of(2024, 12, 25),
                LocalDate.of(2024, 12, 25),
                1L);

        // Then
        assertThat(campaignDetailsByCampaignsIds.size()).isZero();
    }

    @Test
    @DisplayName("getCampaignDetails : Error 3. 해당 옵션의 데이터가 없음")
    void test3() {
        // given
        final LocalDate start = LocalDate.of(2024, 11, 11);
        final LocalDate end = LocalDate.of(2024, 11, 13);

        List<Execution> executions = List.of(
                newExecution(1L, 1L, "상품1", "상품1"),
                newExecution(2L, 2L, "상품2", "상품2"),
                newExecution(3L, 3L, "상품3", "상품3")
        );

        doReturn(executions).when(executionService).findExecutionIdsByCampaignId(any());

        // when
        doReturn(List.of())
                .when(campaignOptionDetailsRepository)
                .findByExecutionIdsAndDateRange(anyList(), any(LocalDate.class), any(LocalDate.class));

        // then
        List<CampaignOptionDetails> rawCampaignDetails = campaignOptionDetailsService.getRawCampaignDetails(start, end, 1L);

        assertThat(rawCampaignDetails.size()).isZero();
    }

    @Test
    @DisplayName("getCampaignDetails : Success 1. 기간에 맞으면서 raw 하게 execution 에 맞는 데이터들 가져오기.")
    void test4() {
        // given
        final LocalDate start = LocalDate.of(2024, 11, 11);
        final LocalDate end = LocalDate.of(2024, 11, 13);

        List<Long> executionIds = List.of(1L, 2L, 3L);
        doReturn(executionIds).when(executionService).findExecutionIdsByCampaignId(any());

        Execution execution1 = newExecution(1L, 1L, "Product A", "Category A");
        Execution execution2 = newExecution(2L, 2L, "Product B", "Category B");
        Execution execution3 = newExecution(3L, 3L, "Product C", "Category C");
        System.out.println("execution3 = " + execution1.toString());
        // Mock 데이터: CampaignOptionDetails
        List<CampaignOptionDetails> campaignOptionDetailsList = List.of(
                newCampaignOptionDetails(1L, start, 100L, 50.0, 200.0, 400.0, 2L, 0.5, 2.5, "검색", 3L, execution1),
                newCampaignOptionDetails(2L, end, 200L, 30.0, 150.0, 300.0, 3L, 0.8, 3.0, "비검색", 3L, execution2),
                newCampaignOptionDetails(3L, end.plusDays(1), 200L, 30.0, 150.0, 300.0, 3L, 0.8, 3.0, "비검색", 3L, execution3), // 속하지 않는 데이터
                newCampaignOptionDetails(4L, end, 200L, 30.0, 150.0, 300.0, 3L, 0.8, 3.0, "비검색", 3L, execution3)
        );
        // Mock 설정: 날짜 범위를 기반으로 데이터를 필터링
        doAnswer(data -> {
            List<Long> ids = data.getArgument(0);
            LocalDate from = data.getArgument(1);
            LocalDate to = data.getArgument(2);
            return campaignOptionDetailsList.stream()
                    .filter(detail -> ids.contains(detail.getExecution().getId()) &&
                            (detail.getCopDate().isEqual(from) || detail.getCopDate().isAfter(from)) &&
                            (detail.getCopDate().isEqual(to) || detail.getCopDate().isBefore(to)))
                    .toList();
        }).when(campaignOptionDetailsRepository)
                .findByExecutionIdsAndDateRange(anyList(), any(LocalDate.class), any(LocalDate.class));

        // when
        final List<CampaignOptionDetails> result = campaignOptionDetailsService.getRawCampaignDetails(start, end, 1L);

        System.out.println("result = " + Arrays.toString(result.toArray()));

        // then
        assertThat(result).hasSize(3); // 날짜 범위에 맞는 3개의 데이터가 반환되어야 함

    }

    @Test
    @DisplayName("getCampaignDetails : Success 2. 데이터 계산")
    void test5() {
        // Given
        final LocalDate start = LocalDate.of(2024, 11, 11);
        final LocalDate end = LocalDate.of(2024, 11, 13);

        List<Long> executionIds = List.of(1L, 2L, 3L);
        doReturn(executionIds).when(executionService).findExecutionIdsByCampaignId(any());

        Execution execution1 = newExecution(1L, 1L, "Product A", "Category A");
        Execution execution2 = newExecution(2L, 2L, "Product B", "Category B");

        // Mock 데이터 생성
        List<CampaignOptionDetails> campaignOptionDetailsList = List.of(
                newCampaignOptionDetails(1L, start, 1L, 10.0, 10.0, 10.0, 1L, 0.3, 2.5, "검색", 3L, execution1),
                newCampaignOptionDetails(2L, end, 3L, 500.0, 5000.0, 10.0, 3L, 0.5, 3.0, "비검색", 1L, execution2),
                newCampaignOptionDetails(3L, end, 3L, 500.0, 5000.0, 10.0, 1L, 0.5, 3.0, "비검색", 1L, execution2)
        );
        System.out.println("campaignOptionDetailsList = " + campaignOptionDetailsList.toString());
        doReturn(campaignOptionDetailsList).when(campaignOptionDetailsRepository)
                .findByExecutionIdsAndDateRange(anyList(), eq(start), eq(end));

        // When
        List<CampaignOptionDetailsResponseDto> result = campaignOptionDetailsService.getCampaignDetailsByCampaignsIds(start, end, 1L);
        System.out.println("result = " + result);
        // Then
        System.out.println("result = " + result.get(0).toString());
        assertThat(result.get(0).getCopImpressions()).isEqualTo(1L);
        assertThat(result.get(1).getCopImpressions()).isEqualTo(6L);
        assertThat(result).hasSize(2);

    }


    public Execution newExecution(long l, long executionid, String detail, String name) {
        return Execution.builder()
                .id(l)
                .exeId(executionid)
                .exeDetailCategory(detail)
                .exeProductName(name)
                .build();
    }

    public CampaignOptionDetails newCampaignOptionDetails(
            long id, LocalDate date, Long impressions, Double copadcost, Double copadsales,
            Double coproas, Long copclicks, Double copclickRate, Double copcvr, String copsearchType,
            Long copSales, Execution execution
    ) {
        return CampaignOptionDetails.builder()
                .id(id)
                .copDate(date)
                .copImpressions(impressions)
                .copAdcost(copadcost)
                .copAdsales(copadsales)
                .copRoas(coproas)
                .copClicks(copclicks)
                .copClickRate(copclickRate)
                .copCvr(copcvr)
                .copSearchType(copsearchType)
                .copSales(copSales)
                .execution(execution)
                .build();
    }

    public Member getMember() {
        return Member.builder()
                .email("test@test.com")
                .build();
    }
}