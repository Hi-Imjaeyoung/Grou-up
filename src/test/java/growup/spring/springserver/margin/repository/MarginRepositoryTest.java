package growup.spring.springserver.margin.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.dto.DailyAdSummaryDto;
import growup.spring.springserver.margin.dto.MarginOverviewResponseDto;
import jakarta.transaction.Transactional;
import growup.spring.springserver.margin.dto.DailyNetProfitResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
class MarginRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private MarginRepository marginRepository;

    private Member member;
    private Campaign campaign1, campaign2, campaign3,campaign4;

    @BeforeEach
    void setup() {
        member = memberRepository.save(newMember());
        campaign1 = campaignRepository.save(newCampaign(member, 1L));
        campaign2 = campaignRepository.save(newCampaign(member, 2L));
        campaign3 = campaignRepository.save(newCampaign(member, 3L));
        campaign4 = campaignRepository.save(newCampaign(member, 4L));


        marginRepository.save(newMargin(LocalDate.of(2024, 11, 10), campaign1, 100L, 200.0, 50L,100.0));
        marginRepository.save(newMargin(LocalDate.of(2024, 11, 11), campaign1, 120L, 250.0, 60L,200.0));
        marginRepository.save(newMargin(LocalDate.of(2024, 11, 10), campaign2, 150L, 300.0, 75L,100.0));
        marginRepository.save(newMargin(LocalDate.of(2024, 11, 2), campaign4, 150L, 300.0, 75L,250.0));
    }

    @Test
    @DisplayName("findByCampaignIdsAndDates(): Success case - Margins found")
    void findByCampaignIdsAndDates_test1() {
        // Given
        List<Long> campaignIds = List.of(campaign1.getCampaignId(), campaign2.getCampaignId());
        LocalDate start = LocalDate.of(2024, 11, 10);
        LocalDate end = LocalDate.of(2024, 11, 11);

        // When
        List<Margin> margins = marginRepository.findByCampaignIdsAndDates(campaignIds, start, end);

        // Then
        assertThat(margins)
                .hasSize(3)
                .anyMatch(m -> m.getCampaign().getCampaignId().equals(campaign1.getCampaignId()))
                .anyMatch(m -> m.getCampaign().getCampaignId().equals(campaign2.getCampaignId()))
                .noneMatch(m -> m.getCampaign().getCampaignId().equals(campaign3.getCampaignId()));
    }

    @Test
    @DisplayName("findMarginOverviewGraphByCampaignIdsAndDate(): Success case 1")
    void findMarginOverviewGraphByCampaignIdsAndDate(){
        // Given
        List<Long> campaignIds = List.of(
                campaign1.getCampaignId(),
                campaign2.getCampaignId(),
                campaign3.getCampaignId(),
                campaign4.getCampaignId());
        LocalDate start = LocalDate.of(2024, 11, 3);
        LocalDate end = LocalDate.of(2024, 11, 10);

        // when
        List<DailyAdSummaryDto> marginRepository7daysTotalsByCampaignIds = marginRepository.findMarginOverviewGraphByCampaignIdsAndDate(campaignIds, start, end);
        // then
        assertThat(marginRepository7daysTotalsByCampaignIds).hasSize(1);
        assertThat(marginRepository7daysTotalsByCampaignIds.get(0).getMarSales()).isEqualTo(200.0);
    }
    @Test
    @DisplayName("findByCampaignIdsAndDates(): error 1. No matching margins")
    void test2() {
        // Given
        List<Long> campaignIds = List.of(campaign3.getCampaignId());
        LocalDate start = LocalDate.of(2024, 11, 10);
        LocalDate end = LocalDate.of(2024, 11, 11);

        // When
        List<Margin> margins = marginRepository.findByCampaignIdsAndDates(campaignIds, start, end);

        // Then
        assertThat(margins).isEmpty();
    }

    @Test
    @DisplayName("findByCampaignIdsAndDates(): error 2.  No matching dates")
    void test3() {
        // Given
        List<Long> campaignIds = List.of(campaign1.getCampaignId(), campaign2.getCampaignId());
        LocalDate start = LocalDate.of(2024, 11, 13);
        LocalDate end = LocalDate.of(2024, 11, 14);

        // When
        List<Margin> margins = marginRepository.findByCampaignIdsAndDates(campaignIds, start, end);

        // Then
        assertThat(margins).isEmpty();
    }

    @Test
    @DisplayName("findByCampaignIdAndDates() : SuccessCase ")
    void test4() {

        LocalDate start = LocalDate.of(2024, 11, 10);
        LocalDate end = LocalDate.of(2024, 11, 10);

        List<Margin> margins = marginRepository.findByCampaignIdAndDates(campaign1.getCampaignId(), start, end);

        assertThat(margins).hasSize(1);

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
        marginRepository.save(newMargin(includeDate,campaign1,0L,0.0,0L,0.0));
        marginRepository.save(newMargin(excludeDate,campaign1,0L,0.0,0L,0.0));
        marginRepository.save(newMargin(includeDate,campaign2,0L,0.0,0L,0.0));
        marginRepository.save(newMargin(excludeDate,campaign2,0L,0.0,0L,0.0));
        //given
        final int result = marginRepository.deleteByCampaignIdAndDate(start,end,List.of(1L,2L));
        assertThat(result).isEqualTo(2);
    }
    @DisplayName("findByCampaignIdAndDate() : successCase1.")
    void test4_1() {
        LocalDate date = LocalDate.of(2024, 11, 10);
        Long campaignId = campaign1.getCampaignId();

        Margin margin = marginRepository.findByCampaignIdAndDate(campaignId, date).get();
        assertAll(
                () -> assertThat(margin.getMarDate()).isEqualTo(LocalDate.of(2024, 11, 10)),
                () -> assertThat(margin.getMarImpressions()).isEqualTo(100L),
                () -> assertThat(margin.getCampaign().getCampaignId()).isEqualTo(1L),
                () -> assertThat(margin.getMarAdConversionSales()).isEqualTo(50L)
        );
    }

    @Test
    @DisplayName("findTotalMarginByDateRangeAndEmail() : successCase")
    void test5() {
        LocalDate start = LocalDate.of(2024, 11, 8);
        LocalDate end = LocalDate.of(2024, 11, 10);
        String email = "test@test.com";


        marginRepository.save(newMargin(LocalDate.of(2024, 11, 8), campaign1, 100.0, 100.0));
        marginRepository.save(newMargin(LocalDate.of(2024, 11, 9), campaign1, 20.0, 25.0));
        marginRepository.save(newMargin(LocalDate.of(2024, 11, 10), campaign1, 30.0, 25.0));

        marginRepository.save(newMargin(LocalDate.of(2024, 11, 8), campaign2, 50.0, 30.0));
        marginRepository.save(newMargin(LocalDate.of(2024, 11, 9), campaign2, 50.0, 15.0));
        marginRepository.save(newMargin(LocalDate.of(2024, 11, 10), campaign2, 50.0, 20.0));


        List<DailyNetProfitResponseDto> totalMarginByDateRangeAndEmail = marginRepository.findTotalMarginByDateRangeAndEmail(start, end, email);

        assertThat(totalMarginByDateRangeAndEmail).hasSize(3);

        assertThat(totalMarginByDateRangeAndEmail.get(0).getMargin()).isEqualTo(150.0);
        assertThat(totalMarginByDateRangeAndEmail.get(0).getMarReturnCost()).isEqualTo(130.0);
    }

    @Test
    @DisplayName("findLatestMarginDateByEmail() : SuccessCase")
    void test6() {
        String email = "test@test.com";


        Optional<LocalDate> latestMarginDateByEmail = marginRepository.findLatestMarginDateByEmail(email);

        assertThat(latestMarginDateByEmail).contains(LocalDate.of(2024, 11, 11));
    }

    @Test
    @DisplayName("findLatestMarginDateByEmail() : failCase")
    void test7() {
        String email = "nodata@test.com";

        marginRepository.save(newMargin(LocalDate.of(2024, 11, 8), campaign1, 100.0, 100.0));
        marginRepository.save(newMargin(LocalDate.of(2024, 11, 5), campaign1, 20.0, 25.0));
        marginRepository.save(newMargin(LocalDate.of(2024, 11, 10), campaign1, 30.0, 25.0));

        Optional<LocalDate> latestMarginDateByEmail = marginRepository.findLatestMarginDateByEmail(email);

        assertThat(latestMarginDateByEmail).isEmpty();



    }

    @Test
    @DisplayName("findAllByCampaignCampaignIdInAndMarDate() : SuccessCase")
    void findAllByCampaignCampaignIdInAndMarDate() {
        // Given
        List<Long> campaignIds = List.of(campaign1.getCampaignId(), campaign2.getCampaignId());
        LocalDate marDate = LocalDate.of(2024, 11, 10);

        // When
        List<Margin> margins = marginRepository.findAllByCampaignCampaignIdInAndMarDate(campaignIds, marDate);

        // Then
        assertAll(
                () -> assertThat(margins).hasSize(2),
                () -> assertThat(margins).anyMatch(m -> m.getCampaign().getCampaignId().equals(campaign1.getCampaignId())),
                () -> assertThat(margins).anyMatch(m -> m.getCampaign().getCampaignId().equals(campaign2.getCampaignId()))
        );
    }
    @Test
    @DisplayName("findAllByCampaignCampaignIdInAndMarDate() : SuccessCase2")
    void findAllByCampaignCampaignIdInAndMarDate2() {
        List<Long> campaignIds = List.of(campaign1.getCampaignId(), campaign2.getCampaignId());
        LocalDate marDate = LocalDate.of(2024, 11, 29);
        // When
        List<Margin> margins = marginRepository.findAllByCampaignCampaignIdInAndMarDate(campaignIds, marDate);
        // Then
        assertThat(margins).isEmpty();
        System.out.println("margins = " + margins);
    }

    @Test
    @DisplayName("findMarginOverviewByCampaignIdsAndDate () : successCase")
    void findMarginOverviewByCampaignIdsAndDate_success() {

        List<Long> campaignIds = List.of(
                campaign1.getCampaignId(), campaign2.getCampaignId(),
                campaign3.getCampaignId(), campaign4.getCampaignId()
        );

        LocalDate startDate = LocalDate.of(2024, 12,1 );
        LocalDate endDate = LocalDate.of(2024, 12, 30);

        marginRepository.save(dashBoardMargin(LocalDate.of(2024,12,1),campaign1,100.0,200.0,50L,100.0,30L,100.0));
        marginRepository.save(dashBoardMargin(LocalDate.of(2024,12,3),campaign2,100.0,200.0,50L,100.0,30L,500.0));
        marginRepository.save(dashBoardMargin(LocalDate.of(2024,12,5),campaign3,100.0,200.0,50L,100.0,30L,700.0));
        marginRepository.save(dashBoardMargin(LocalDate.of(2024,12,12),campaign1,100.0,200.0,50L,100.0,30L,600.0));
        marginRepository.save(dashBoardMargin(LocalDate.of(2024,12,11),campaign1,100.0,200.0,50L,100.0,30L,400.0));
        marginRepository.save(dashBoardMargin(LocalDate.of(2024,1,25),campaign4,100.0,200.0,50L,100.0,30L,700.0));


        // 1일꺼 빼고 다 나옴

        List<MarginOverviewResponseDto> marginOverviewByCampaignIdsAndDate = marginRepository.findMarginOverviewByCampaignIdsAndDate(startDate, endDate, campaignIds);

        assertThat(marginOverviewByCampaignIdsAndDate)
                .hasSize(3)
                .extracting(MarginOverviewResponseDto::getCampaignId)
                .containsExactly(campaign1.getCampaignId(), campaign3.getCampaignId(), campaign2.getCampaignId()); //

        assertThat(marginOverviewByCampaignIdsAndDate.get(0).getCampaignId()).
                isEqualTo(campaign1.getCampaignId());
        assertThat(marginOverviewByCampaignIdsAndDate.get(0).getMarNetProfit()).isEqualTo(300.0);
        assertThat(marginOverviewByCampaignIdsAndDate.get(0).getMarReturnCost()).isEqualTo(600.0);
        assertThat(marginOverviewByCampaignIdsAndDate.get(0).getMarAdCost()).isEqualTo(300.0);
        // 순이익 / 집행광고비 * 100
        assertThat(marginOverviewByCampaignIdsAndDate.get(0).getMarRoi()).isEqualTo(
                (300.0 / 300.0) * 100);
        assertThat(marginOverviewByCampaignIdsAndDate.get(0).getMarReturnRate()).isEqualTo(166.67);

    }

    @Test
    @DisplayName("findMarginOverviewByCampaignIdsAndDate () ")
    void findDistinctAdvDatesByEmailAndDateBetween() {

        LocalDate startDate = LocalDate.of(2024, 12,2 );
        LocalDate endDate = LocalDate.of(2024, 12, 30);

        marginRepository.save(dashBoardMargin(LocalDate.of(2024,12,1),campaign4,100.0,200.0,50L,100.0,30L,100.0));
        marginRepository.save(dashBoardMargin(LocalDate.of(2024,12,3),campaign2,100.0,200.0,50L,100.0,30L,500.0));
        marginRepository.save(dashBoardMargin(LocalDate.of(2024,12,5),campaign3,100.0,200.0,50L,100.0,30L,700.0));
        marginRepository.save(dashBoardMargin(LocalDate.of(2024,12,12),campaign1,100.0,200.0,50L,100.0,30L,600.0));
        marginRepository.save(dashBoardMargin(LocalDate.of(2024,12,11),campaign1,100.0,200.0,50L,100.0,30L,400.0));


        List<LocalDate> distinctAdvDatesByEmailAndDateBetween = marginRepository.findDistinctAdvDatesByEmailAndDateBetween(member.getEmail(), startDate, endDate);

        assertThat(distinctAdvDatesByEmailAndDateBetween)
                .hasSize(4)
                .extracting(LocalDate::toString)
                .containsExactlyInAnyOrder(
                        "2024-12-03",
                        "2024-12-05",
                        "2024-12-11",
                        "2024-12-12"
                );
    }
    @Test
    @DisplayName("findExistingDatesByCampaignIdAndDateIn () : successCase")
    void findExistingDatesByCampaignIdAndDateIn() {
        List<LocalDate> datesToCheck = List.of(
                LocalDate.of(2024, 11, 10),
                LocalDate.of(2024, 11, 11),
                LocalDate.of(2024, 11, 12)
        );
        Long campaignId = campaign1.getCampaignId();
        Set<LocalDate> existingDatesByCampaignIdAndDateIn = marginRepository.findExistingDatesByCampaignIdAndDateIn(campaignId, datesToCheck);

        assertThat(existingDatesByCampaignIdAndDateIn)
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        LocalDate.of(2024, 11, 10),
                        LocalDate.of(2024, 11, 11)
                );
    }

    private Margin dashBoardMargin(LocalDate date, Campaign campaign, Double marNetProfit, Double marReturnCost, Long marReturnCount, Double marAdCost, Long marAdConversionSalesCount, Double marSales) {
        return Margin.builder()
                .marDate(date)
                .campaign(campaign)
                .marReturnCost(marReturnCost)
                .marNetProfit(marNetProfit)
                .marReturnCount(marReturnCount)
                .marAdCost(marAdCost)
                .marAdConversionSalesCount(marAdConversionSalesCount)
                .marSales(marSales)
                .build();
    }

    private Member newMember() {
        return Member.builder().email("test@test.com").build();
    }

    private Campaign newCampaign(Member member, Long campaignId) {
        return Campaign.builder()
                .member(member)
                .camCampaignName("Test Campaign")
                .campaignId(campaignId)
                .camAdType("Test Type")
                .build();
    }

    private Margin newMargin(LocalDate date, Campaign campaign, Long impressions, Double cost, Long conversions,Double sales) {
        return Margin.builder()
                .marDate(date)
                .campaign(campaign)
                .marImpressions(impressions)
                .marAdCost(cost)
                .marSales(sales)
                .marAdConversionSales(conversions)
                .build();
    }

    private Margin newMargin(LocalDate date, Campaign campaign, Double marNetProfit, Double marReturnCost) {
        return Margin.builder()
                .marDate(date)
                .campaign(campaign)
                .marReturnCost(marReturnCost)
                .marNetProfit(marNetProfit)
                .build();
    }
}
