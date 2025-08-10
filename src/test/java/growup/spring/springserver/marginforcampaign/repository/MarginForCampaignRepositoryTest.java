package growup.spring.springserver.marginforcampaign.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class MarginForCampaignRepositoryTest {
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private MarginForCampaignRepository marginForCampaignRepository;
    @Autowired
    private MemberRepository memberRepository;

    @DisplayName("save success")
    @Test
    void test1() {
        final MarginForCampaign marginForCampaign = getgetMarginForCampaign(getCampaign("송보석",1L), "모자", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH);
        MarginForCampaign result = marginForCampaignRepository.save(marginForCampaign);

        assertThat(result.getCampaign().getCamCampaignName()).isEqualTo("송보석");
    }

    @DisplayName("MarginForCampaignByCampaignId ")
    @Test
    void test2() {
        Campaign campaign = campaignRepository.save(getCampaign("송보석",1L));
        Campaign IncorrectCampaign = campaignRepository.save(getCampaign("Not송보석",2L));
//        given
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign, "모자", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign, "모자2", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign, "모자3", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));

        marginForCampaignRepository.save(getgetMarginForCampaign(IncorrectCampaign, "모자3", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));

//        when
        List<MarginForCampaign> result = marginForCampaignRepository.MarginForCampaignByCampaignId(1L);
//        then
        assertThat(result).hasSize(3);
        for (MarginForCampaign data : result) {
            assertThat(data.getCampaign().getCamCampaignName()).isEqualTo("송보석");
        }
    }

    @DisplayName("findByEmailAndMfcProductName: 조회, 수정 한 번에 해당 캠페인의 중복이름은 가능 (수정해야하니까) ")
    @Test
    void test3() {
        // given
        Member member = getMember();
        memberRepository.save(member);

        Campaign campaign = campaignRepository.save(getCampaign("송보석",1L,member));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign, "모자", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign, "모자2", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));

        Campaign campaign2 = campaignRepository.save(getCampaign("송보석1",2L,member));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign2, "모자3", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign2, "모자4", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));

        Campaign campaign3 = campaignRepository.save(getCampaign("송보석2",3L,member));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign3, "모자5", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign3, "모자6", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));

        Campaign campaign4 = campaignRepository.save(getCampaign("송보석3",4L,member));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign4, "모자7", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign4, "모자8", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));

        // 3 캠페인은 5,6 가지고 있음 8 다른곳에 있음 존재해야함

        Optional<MarginForCampaign> emptyResult= marginForCampaignRepository.findByEmailAndMfcProductNameExcludingCampaign("fa7271@naver.com", "모자8",3L, MarginType.ROCKET_GROWTH);
        // 내꺼니까 상관없음
        Optional<MarginForCampaign> notEmptyResult = marginForCampaignRepository.findByEmailAndMfcProductNameExcludingCampaign("fa7271@naver.com", "모자",1L,MarginType.ROCKET_GROWTH);


        //then
        assertThat(emptyResult).isPresent(); // 다른 캠페인에 있으니 중복 생성 불가능
        assertThat(notEmptyResult).isEmpty(); // 현재 캠페인에  있는거니깐 패스해야함(수정)
    }

    @DisplayName("deleteById() : Success")
    @Test
    void test4() {
        Member member = getMember();
        memberRepository.save(member);

        Campaign campaign = campaignRepository.save(getCampaign("송보석",1L,member));
        MarginForCampaign build = MarginForCampaign.builder().id(1L).build();
        marginForCampaignRepository.save(build);


        marginForCampaignRepository.deleteById(1L);

        assertThat(marginForCampaignRepository.existsById(1L)).isFalse();
    }

    @DisplayName("findAllByMemberEmailWithFetch : success")
    @Test
    void test5() {
        Member member = getMember();
        memberRepository.save(member);

        Campaign campaign = campaignRepository.save(getCampaign("송보석",1L,member));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign, "모자", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign, "모자2", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));

        Campaign campaign2 = campaignRepository.save(getCampaign("송보석1",2L,member));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign2, "모자3", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign2, "모자4", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));

        Campaign campaign3 = campaignRepository.save(getCampaign("송보석2",3L,member));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign3, "모자5", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));
        marginForCampaignRepository.save(getgetMarginForCampaign(campaign3, "모자6", 1L, 1L, 1L, 1.1,MarginType.ROCKET_GROWTH));


        List<MarginForCampaign> result = marginForCampaignRepository.findAllByMemberEmailWithFetch("fa7271@naver.com");

        assertThat(result).hasSize(6);


    }

    public Member getMember() {
        return Member.builder()
                .email("fa7271@naver.com")
                .build();
    }
    public Campaign getCampaign(String name, Long id) {
        return Campaign.builder()
                .campaignId(id)
                .camCampaignName(name)
                .build();
    }
    public Campaign getCampaign(String name, Long id, Member member) {
        return Campaign.builder()
                .member(member)
                .campaignId(id)
                .camCampaignName(name)
                .build();
    }

    public MarginForCampaign getgetMarginForCampaign(Campaign campaign, String productName, Long mfcTotalPrice, Long mfcCostPrice, Long mfcPerPiece, Double mfcZeroRoas, MarginType marginType ) {
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
}