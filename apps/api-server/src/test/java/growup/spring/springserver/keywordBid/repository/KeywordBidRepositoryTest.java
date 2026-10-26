package growup.spring.springserver.keywordBid.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.keywordBid.domain.KeywordBid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class KeywordBidRepositoryTest {
    @Autowired
    private KeywordBidRepository keywordBidRepository;
    @Autowired
    private CampaignRepository campaignRepository;

    private Campaign campaign;

    @BeforeEach
    void setUp(){
        campaign = campaignRepository.save(Campaign.builder()
                        .campaignId(1L)
                        .camCampaignName("test")
                        .build());
    }

    @DisplayName("save() : success ")
    @Test
    void test1_1(){
        //when
        KeywordBid keywordBid = getKeywordBid(campaign,1000L,"testKey");
        //given
        final KeywordBid result = keywordBidRepository.save(keywordBid);
        //then
        assertThat(result.getBid()).isEqualTo(1000L);
        assertThat(result.getKeyword()).isEqualTo("testKey");
        assertThat(result.getCampaign().getCampaignId()).isEqualTo(1L);
    }

    @DisplayName("savaALl() : success")
    @Test
    void test2_1(){
        //when
        KeywordBid keywordBid = getKeywordBid(campaign,1000L, "testKey2");
        KeywordBid keywordBid2 = getKeywordBid(campaign,1000L, "testKey2");
        KeywordBid keywordBid3 = getKeywordBid(campaign,1000L, "testKey2");
        KeywordBid keywordBid4 = getKeywordBid(campaign,1000L, "testKey2");
        List<KeywordBid> keywordBids = List.of(keywordBid,keywordBid2,keywordBid3,keywordBid4);
        //given
        keywordBidRepository.saveAll(keywordBids);
        final List<KeywordBid> result = keywordBidRepository.findAll();
        //then
        assertThat(result).hasSize(4);
        assertThat(result.get(0).getCampaign().getCampaignId()).isEqualTo(1L);
    }

    @DisplayName("findByCampaign_CampaignIdANDKeyword : success")
    @Test
    void test3_1(){
        //when
        KeywordBid keywordBid = getKeywordBid(campaign,1000L, "testKey");
        KeywordBid keywordBid2 = getKeywordBid(campaign,2000L, "testKey2");
        KeywordBid keywordBid3 = getKeywordBid(campaign,3000L, "testKey3");
        KeywordBid keywordBid4 = getKeywordBid(campaign,4000L, "testKey4");
        List<KeywordBid> keywordBids = List.of(keywordBid,keywordBid2,keywordBid3,keywordBid4);
        keywordBidRepository.saveAll(keywordBids);
        //given
        final KeywordBid result = keywordBidRepository.findByCampaign_CampaignIdANDKeyword(campaign.getCampaignId(),"testKey2").get();
        //then
        assertThat(result.getBid()).isEqualTo(2000L);
    }

    @DisplayName("existByCampaign_CampaignIdANDKeyword : success 1. 값이 존재")
    @Test
    void test4_1(){
        //when
        KeywordBid keywordBid = getKeywordBid(campaign,1000L, "testKey");
        KeywordBid keywordBid2 = getKeywordBid(campaign,2000L, "testKey2");
        KeywordBid keywordBid3 = getKeywordBid(campaign,3000L, "testKey3");
        KeywordBid keywordBid4 = getKeywordBid(campaign,4000L, "testKey4");
        List<KeywordBid> keywordBids = List.of(keywordBid,keywordBid2,keywordBid3,keywordBid4);
        keywordBidRepository.saveAll(keywordBids);
        //given
        final boolean result = keywordBidRepository.existsByCampaign_CampaignIdAndKeyword(campaign.getCampaignId(),"testKey2");
        //then
        assertThat(result).isEqualTo(true);
    }

    @DisplayName("existByCampaign_CampaignIdANDKeyword : success 2. 값이 미 존재")
    @Test
    void test4_2(){
        //when
        KeywordBid keywordBid = getKeywordBid(campaign,1000L, "testKey");
        KeywordBid keywordBid2 = getKeywordBid(campaign,2000L, "testKey2");
        KeywordBid keywordBid3 = getKeywordBid(campaign,3000L, "testKey3");
        KeywordBid keywordBid4 = getKeywordBid(campaign,4000L, "testKey4");
        List<KeywordBid> keywordBids = List.of(keywordBid,keywordBid2,keywordBid3,keywordBid4);
        keywordBidRepository.saveAll(keywordBids);
        //given
        final boolean result = keywordBidRepository.existsByCampaign_CampaignIdAndKeyword(campaign.getCampaignId(),"testKey5");
        //then
        assertThat(result).isEqualTo(false);
    }

    @DisplayName("findAllByCampaignId() : success")
    @Test
    void test5_1(){
        //when
        KeywordBid keywordBid = getKeywordBid(campaign,1000L, "testKey");
        KeywordBid keywordBid2 = getKeywordBid(campaign,2000L, "testKey2");
        KeywordBid keywordBid3 = getKeywordBid(campaign,3000L, "testKey3");
        KeywordBid keywordBid4 = getKeywordBid(campaign,4000L, "testKey4");
        List<KeywordBid> keywordBids = List.of(keywordBid,keywordBid2,keywordBid3,keywordBid4);
        keywordBidRepository.saveAll(keywordBids);
        //given
        final List<KeywordBid> results = keywordBidRepository.findAllByCampaign_CampaignId(1L);
        //then
        assertThat(results).hasSize(4);
    }

    @DisplayName("DeleteByCampaign_CampaignIDANDKeyword() : success")
    @Test
    void test6_1(){
        //when
        KeywordBid keywordBid = getKeywordBid(campaign,1000L, "testKey");
        KeywordBid keywordBid2 = getKeywordBid(campaign,2000L, "testKey2");
        KeywordBid keywordBid3 = getKeywordBid(campaign,3000L, "testKey3");
        KeywordBid keywordBid4 = getKeywordBid(campaign,4000L, "testKey4");
        List<KeywordBid> keywordBids = List.of(keywordBid,keywordBid2,keywordBid3,keywordBid4);
        keywordBidRepository.saveAll(keywordBids);
        //given
        final int result = keywordBidRepository.deleteByCampaign_CampaignIdANDKeyword(campaign.getCampaignId(),"testKey");
        //then
        assertThat(result).isEqualTo(1);
    }


    public static KeywordBid getKeywordBid(Campaign campaign, Long bid, String keyword){
        return KeywordBid.builder()
                .campaign(campaign)
                .bid(bid)
                .keyword(keyword)
                .build();
    }
}
