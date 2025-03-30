package growup.spring.springserver.margin.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.dto.DailyAdSummaryDto;
import jakarta.transaction.Transactional;
import growup.spring.springserver.margin.dto.DailyNetProfitResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;

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
        assertThat(margins).hasSize(3);
        assertThat(margins).anyMatch(m -> m.getCampaign().getCampaignId().equals(campaign1.getCampaignId()));
        assertThat(margins).anyMatch(m -> m.getCampaign().getCampaignId().equals(campaign2.getCampaignId()));
        assertThat(margins).noneMatch(m -> m.getCampaign().getCampaignId().equals(campaign3.getCampaignId()));
    }

    @Test
    @DisplayName("find7daysTotalsByCampaignIds(): Success case 1")
    void find7daysTotalsByCampaignIds_test1(){
        // Given
        List<Long> campaignIds = List.of(
                campaign1.getCampaignId(),
                campaign2.getCampaignId(),
                campaign3.getCampaignId(),
                campaign4.getCampaignId());
        LocalDate start = LocalDate.of(2024, 11, 3);
        LocalDate end = LocalDate.of(2024, 11, 10);

        // when
        List<DailyAdSummaryDto> marginRepository7daysTotalsByCampaignIds = marginRepository.find7daysTotalsByCampaignIds(campaignIds, start, end);
        // then
        assertThat(marginRepository7daysTotalsByCampaignIds).hasSize(1);
        assertThat(marginRepository7daysTotalsByCampaignIds.get(0).getMarAdCost()).isEqualTo(500.0);
        assertThat(marginRepository7daysTotalsByCampaignIds.get(0).getMarSales()).isEqualTo(200.0);
        assertThat(marginRepository7daysTotalsByCampaignIds.get(0).getMarRoas()).isEqualTo(40.0);
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
    @DisplayName("findTotalMarginByDateRangeAndEmail() : SuccessCase")
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
