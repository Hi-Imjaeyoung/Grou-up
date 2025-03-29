package growup.spring.springserver.campaignoptiondetails.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.campaignoptiondetails.domain.CampaignOptionDetails;
import growup.spring.springserver.execution.domain.Execution;
import growup.spring.springserver.execution.repository.ExecutionRepository;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CampaignOptionDetailsRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private CampaignOptionDetailsRepository campaignOptionDetailsRepository;

    Member member;
    Campaign campaign;
    Execution execution1,execution2;

    CampaignOptionDetails optionDetails1, optionDetails2, optionDetails3;
    private LocalDate start;
    private LocalDate end ;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(newMember());
        campaign = campaignRepository.save(newCampaign(member));
        execution1 = executionRepository.save(newExecution(campaign, 91130550615L, "블랙", "베리픽스 휴대용 안경 케이스 가죽 파우치 안경집,블랙"));
        execution2 = executionRepository.save(newExecution(campaign, 91130550622L, "화이트", "베리픽스 휴대용 안경 케이스 가죽 파우치 안경집,화이트"));
        List<Long> executionIds = List.of(execution1.getId(), execution2.getId());

        optionDetails1 = campaignOptionDetailsRepository.save(newCampaignOptionDetails(execution1, LocalDate.of(2024, 12, 25), 100L, 5L, 1000.0, 5000.0, 500.0, 50L, 5.0, 10.0, "검색"));
        optionDetails2 = campaignOptionDetailsRepository.save(newCampaignOptionDetails(execution2, LocalDate.of(2024, 12, 27), 50L, 3L, 500.0, 2000.0, 400.0, 30L, 3.0, 7.0, "비검색"));
        optionDetails3 = campaignOptionDetailsRepository.save(newCampaignOptionDetails(execution1, LocalDate.of(2025, 12, 29), 80L, 2L, 800.0, 3200.0, 400.0, 40L, 4.0, 8.0, "검색"));

    }

    @Test
    void success_createDB() {
        assertThat(campaignOptionDetailsRepository).isNotNull();
    }

    @Test
    void success_saveData() {
        // Given
        campaignOptionDetailsRepository.save(newCampaignOptionDetails(execution1, LocalDate.of(2025, 12, 30), 80L, 2L, 800.0, 3200.0, 400.0, 40L, 4.0, 8.0, "검색"));
        // when
        List<CampaignOptionDetails> optionDetailsList = campaignOptionDetailsRepository.findAll();

        // Then
        assertThat(optionDetailsList).hasSize(4); // 개선된 방식
    }

    @Test
    @DisplayName("findByExecutionIdsAndDateRange() : Success 1. 날짜에 대한 오류")
    void test_findByExecutionIdsAndDateRange_reverseDates() {
        // Given
        start = LocalDate.of(2024, 12, 25);
        end = LocalDate.of(2024, 12, 20);

        List<Long> executionIds = List.of(execution1.getId(), execution2.getId());

        // When
        List<CampaignOptionDetails> result = campaignOptionDetailsRepository.findByExecutionIdsAndDateRange(executionIds, start, end);

        // Then
        assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("findByExecutionIdsAndDateRange() : Success 2. ExecutionIds 가 없는경우 ")
    void test2() {
        // given

        start = LocalDate.of(2024, 12, 25);
        end = LocalDate.of(2024, 12, 31);

        // when
        List<Long> executionIds = List.of();

        // When
        List<CampaignOptionDetails> result = campaignOptionDetailsRepository.findByExecutionIdsAndDateRange(executionIds, start, end);

        // Then
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("findByExecutionIdsAndDateRange() : Success 3. 해당하는 데이터가 없는 경우")
    void test3() {
        // Given
        start = LocalDate.of(2023, 12, 20);
        end = LocalDate.of(2023, 12, 24);

        List<Long> executionIds = List.of(execution1.getId(), execution2.getId());

        // When
        List<CampaignOptionDetails> result = campaignOptionDetailsRepository.findByExecutionIdsAndDateRange(executionIds, start, end);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByExecutionIdsAndDateRange() : Success 3. Total 특정 날짜 범위에서 데이터 조회 성공")
    void test4() {
        // Given
        start = LocalDate.of(2024, 12, 24);
        end = LocalDate.of(2024, 12, 28);

        List<Long> executionIds = List.of(execution1.getId(), execution2.getId());

        // When
        List<CampaignOptionDetails> result = campaignOptionDetailsRepository.findByExecutionIdsAndDateRange(executionIds, start, end);

        // Then
        assertThat(result).hasSize(2); // 범위 내 데이터 2개
    }

    @Test
    @DisplayName("deleteByCampaignIdAndDate()")
    @Transactional
    void deleteByCampaignIdAndDate(){
        //when
        LocalDate start = LocalDate.parse("2025-03-01", DateTimeFormatter.ISO_DATE);
        LocalDate end = LocalDate.parse("2025-03-29",DateTimeFormatter.ISO_DATE);
        LocalDate includeDate = LocalDate.parse("2025-03-20",DateTimeFormatter.ISO_DATE);
        LocalDate excludeDate = LocalDate.parse("2025-04-01",DateTimeFormatter.ISO_DATE);
        campaignOptionDetailsRepository.save(newCampaignOptionDetails(execution1, includeDate, 100L, 5L, 1000.0, 5000.0, 500.0, 50L, 5.0, 10.0, "검색"));
        campaignOptionDetailsRepository.save(newCampaignOptionDetails(execution2, excludeDate, 100L, 5L, 1000.0, 5000.0, 500.0, 50L, 5.0, 10.0, "검색"));
        List<Long> executionIds = List.of(execution1.getId(), execution2.getId());
        //given
        final int result = campaignOptionDetailsRepository.deleteByCampaignIdAndDate(start,end,executionIds);
        assertThat(result).isEqualTo(1);
    }

    public Member newMember() {
        return Member.builder().email("test@test.com").build();
    }

    public Campaign newCampaign(Member member) {
        return Campaign.builder()
                .member(member)
                .camCampaignName("방한 마스크")
                .campaignId(103130837L)
                .camAdType("Test")
                .build();
    }

    public Execution newExecution(Campaign campaign, long l, String detail, String name) {
        return Execution.builder()
                .exeId(l)
                .exeDetailCategory(detail)
                .exeProductName(name)
                .campaign(campaign)
                .build();
    }

    private CampaignOptionDetails newCampaignOptionDetails(Execution execution, LocalDate date, Long impressions, Long sales, Double adCost, Double adSales, Double roas, Long clicks, Double clickRate, Double cvr, String searchType) {
        return CampaignOptionDetails.builder()
                .execution(execution)
                .copDate(date)
                .copImpressions(impressions)
                .copSales(sales)
                .copAdcost(adCost)
                .copAdsales(adSales)
                .copRoas(roas)
                .copClicks(clicks)
                .copClickRate(clickRate)
                .copCvr(cvr)
                .copSearchType(searchType)
                .build();
    }
}