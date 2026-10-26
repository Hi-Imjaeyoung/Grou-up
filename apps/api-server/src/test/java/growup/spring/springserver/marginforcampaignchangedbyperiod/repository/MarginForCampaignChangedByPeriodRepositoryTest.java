package growup.spring.springserver.marginforcampaignchangedbyperiod.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import growup.spring.springserver.marginforcampaignchangedbyperiod.domain.MarginForCampaignChangedByPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
class MarginForCampaignChangedByPeriodRepositoryTest {

    @Autowired
    private MarginForCampaignChangedByPeriodRepository marginForCampaignChangedByPeriodRepository;

    @Autowired
    private MarginForCampaignRepository marginForCampaignRepository;
    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;
    private MarginForCampaign marginForCampaign1, marginForCampaign2, marginForCampaign11, marginForCampaign3;

    // 1명의 멤버
    // 3개의 켐피임
    // 1번 켐피임에는 2개의 MarginForCampaign 그외는 1개의 MarginForCampaign
    @BeforeEach
    void setup() {
        member = getMember();
        memberRepository.save(member);

        Campaign campaign = campaignRepository.save(getCampaign("송보석", 1L, member));
        marginForCampaign1 = marginForCampaignRepository.save(getgetMarginForCampaign(campaign, "모자", 1L, 1L, 1L, 1.1, MarginType.ROCKET_GROWTH));
        marginForCampaign11 = marginForCampaignRepository.save(getgetMarginForCampaign(campaign, "모자2", 1L, 1L, 1L, 1.1, MarginType.ROCKET_GROWTH));

        Campaign campaign2 = campaignRepository.save(getCampaign("송보석1", 2L, member));
        marginForCampaign2 = marginForCampaignRepository.save(getgetMarginForCampaign(campaign2, "모자3", 1L, 1L, 1L, 1.1, MarginType.ROCKET_GROWTH));

        Campaign campaign3 = campaignRepository.save(getCampaign("송보석2", 3L, member));
        marginForCampaign3 = marginForCampaignRepository.save(getgetMarginForCampaign(campaign3, "모자5", 1L, 1L, 1L, 1.1, MarginType.ROCKET_GROWTH));


        marginForCampaignChangedByPeriodRepository.saveAll(List.of(
                newMarginForCampaignChangedByPeriod(marginForCampaign1, LocalDate.of(2023, 1, 1), 100L, 200L, 50L, 10L),
                newMarginForCampaignChangedByPeriod(marginForCampaign1, LocalDate.of(2023, 1, 2), 100L, 200L, 50L, 10L),
                newMarginForCampaignChangedByPeriod(marginForCampaign1, LocalDate.of(2023, 1, 3), 100L, 200L, 50L, 10L),
                newMarginForCampaignChangedByPeriod(marginForCampaign11, LocalDate.of(2023, 1, 1), 100L, 200L, 50L, 10L),
                newMarginForCampaignChangedByPeriod(marginForCampaign11, LocalDate.of(2023, 1, 2), 100L, 200L, 50L, 10L),

                newMarginForCampaignChangedByPeriod(marginForCampaign2, LocalDate.of(2023, 1, 1), 100L, 200L, 50L, 10L),
                newMarginForCampaignChangedByPeriod(marginForCampaign3, LocalDate.of(2023, 1, 1), 100L, 200L, 50L, 10L)
        ));
    }

    @Test
    @DisplayName("Save Test : success")
    void Save() {
        List<MarginForCampaignChangedByPeriod> result = marginForCampaignChangedByPeriodRepository.findAll();

        assertThat(result).hasSize(7);

    }


    @Test
    @DisplayName("Find By MarginForCampaign Id And Date : success1. 존재하는 경우")
    void findByMarginForCampaignIdAndDate_Success1() {

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 10);
        Long marginForCampaignId = marginForCampaign1.getId();

        List<MarginForCampaignChangedByPeriod> byMarginForCampaignIdAndDate = marginForCampaignChangedByPeriodRepository
                .findAllByMarginForCampaign_IdAndDateRange(marginForCampaignId, startDate, endDate);

        assertThat(byMarginForCampaignIdAndDate).hasSize(3);


    }

    @Test
    @DisplayName("Find By MarginForCampaign Id And Date : success2. 존재하지 않는경우")
    void findByMarginForCampaignIdAndDate_success2() {

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 10);

        Long marginForCampaignId = 5L;

        List<MarginForCampaignChangedByPeriod> byMarginForCampaignIdAndDate = marginForCampaignChangedByPeriodRepository
                .findAllByMarginForCampaign_IdAndDateRange(marginForCampaignId, startDate, endDate);

        assertThat(byMarginForCampaignIdAndDate).isEmpty();
    }
    @Test
    @DisplayName("Find By MfcCbp Ids And Date Range : success")
    void findAllByMfcCbpIdsAndDateRange_success() {
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 3);
        List<Long> mfcCbpIds = List.of(marginForCampaign1.getId(), marginForCampaign11.getId());

        List<MarginForCampaignChangedByPeriod> result = marginForCampaignChangedByPeriodRepository
                .findAllByMfcIdsAndDateRange(mfcCbpIds, startDate, endDate);

        assertThat(result).hasSize(5);
    }

    private Member getMember() {
        return Member.builder().email("test@test.com").build();
    }

    public Campaign getCampaign(String name, Long id, Member member) {
        return Campaign.builder()
                .campaignId(id)
                .camCampaignName(name)
                .member(member)
                .build();
    }

    public MarginForCampaign getgetMarginForCampaign(Campaign campaign, String productName, Long mfcTotalPrice, Long mfcCostPrice, Long mfcPerPiece, Double mfcZeroRoas, MarginType marginType) {
        return MarginForCampaign.builder()
                .mfcProductName(productName)
                .mfcTotalPrice(mfcTotalPrice)
                .mfcCostPrice(mfcCostPrice)
                .mfcPerPiece(mfcPerPiece)
                .mfcZeroRoas(mfcZeroRoas)
                .campaign(campaign)
                .mfcType(marginType)
                .build();
    }

    private MarginForCampaignChangedByPeriod newMarginForCampaignChangedByPeriod(
            MarginForCampaign marginForCampaign, LocalDate date, Long salePrice, Long totalPrice, Long costPrice, Long returnPrice) {
        return MarginForCampaignChangedByPeriod.builder()
                .marginForCampaign(marginForCampaign)
                .date(date)
                .costPrice(costPrice)
                .salePrice(salePrice)
                .totalPrice(totalPrice)
                .returnPrice(returnPrice)
                .build();
    }
}