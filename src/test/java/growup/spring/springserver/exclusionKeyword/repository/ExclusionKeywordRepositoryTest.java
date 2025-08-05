package growup.spring.springserver.exclusionKeyword.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.exclusionKeyword.domain.ExclusionKeyword;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class ExclusionKeywordRepositoryTest {
    @Autowired
    private ExclusionKeywordRepository exclusionKeywordRepository;
    @Autowired
    private CampaignRepository campaignRepository;

    @DisplayName("save() : Success")
    @Test
    void test1(){
        //when
        final ExclusionKeyword exclusionKeyword = getExclusionKeyword("exclusionKey",null);
        //given
        ExclusionKeyword result = exclusionKeywordRepository.save(exclusionKeyword);
        //then
        assertThat(result.getExclusionKeyword()).isEqualTo("exclusionKey");
    }
    @DisplayName("findAllByCampaignId() : Success")
    @Test
    void test2(){
        //when
        final Campaign campaign = campaignRepository.save(getCampaign("campaign"));
        exclusionKeywordRepository.save(getExclusionKeyword("exclusionKey1",campaign));
        exclusionKeywordRepository.save(getExclusionKeyword("exclusionKey2",campaign));
        exclusionKeywordRepository.save(getExclusionKeyword("exclusionKey3",campaign));
        //given
        final List<ExclusionKeyword> result = exclusionKeywordRepository.findAllByCampaign_campaignId(1L);
        //then
        assertThat(result.size()).isEqualTo(3);
    }

    @DisplayName("deleteByCampaign_campaignIdANDExclusionKeyword() : Success 1. 만일 데이터가 없을때")
    @Test
    void test4(){
        //when
        final Campaign campaign = campaignRepository.save(getCampaign("campaign"));
        ExclusionKeyword exclusionKeyword = exclusionKeywordRepository.save(getExclusionKeyword("exclusionKey1",campaign));
        exclusionKeywordRepository.save(getExclusionKeyword("exclusionKey2",campaign));
        exclusionKeywordRepository.save(getExclusionKeyword("exclusionKey3",campaign));
        //given
        int result = exclusionKeywordRepository.deleteByCampaign_campaignIdANDExclusionKeyword(2L,exclusionKeyword.getExclusionKeyword());
        //then
        assertThat(result).isEqualTo(0);
    }

    @DisplayName("deleteByCampaign_campaignIdANDExclusionKeyword() : Success 2. 삭제 성공")
    @Test
    void test3(){
        //when
        final Campaign campaign = campaignRepository.save(getCampaign("campaign"));
        ExclusionKeyword exclusionKeyword = exclusionKeywordRepository.save(getExclusionKeyword("exclusionKey1",campaign));
        exclusionKeywordRepository.save(getExclusionKeyword("exclusionKey2",campaign));
        exclusionKeywordRepository.save(getExclusionKeyword("exclusionKey3",campaign));
        //given
        int result = exclusionKeywordRepository.deleteByCampaign_campaignIdANDExclusionKeyword(exclusionKeyword.getCampaign().getCampaignId(),exclusionKeyword.getExclusionKeyword());
        //then
        assertThat(result).isEqualTo(1);
    }

    @DisplayName("existsByExclusionKeyword() : Success")
    @Test
    void test5(){
        //when
        final Campaign campaign = campaignRepository.save(getCampaign("campaign"));
        exclusionKeywordRepository.save(getExclusionKeyword("exclusionKey1",campaign));
        exclusionKeywordRepository.save(getExclusionKeyword("exclusionKey2",campaign));
        exclusionKeywordRepository.save(getExclusionKeyword("exclusionKey3",campaign));
        //given
        final boolean result = exclusionKeywordRepository.existsByExclusionKeyword("exclusionKey1",1L);
        //then
        assertThat(result).isEqualTo(true);
    }

    @DisplayName("existsByExclusionKeyword() : 대소문자 구분? ")
    @Test
    // H2 DB 에서는 대소문자를 구분.
    void test6(){
        //when
        final Campaign campaign = campaignRepository.save(getCampaign("campaign"));
        exclusionKeywordRepository.save(getExclusionKeyword("A",campaign));
        //given
        final boolean result = exclusionKeywordRepository.existsByExclusionKeyword("a",1L);
        //then
        assertThat(result).isEqualTo(false);
    }

    public ExclusionKeyword getExclusionKeyword(String exclusionKeyword, Campaign campaign){
        return ExclusionKeyword.builder()
                .campaign(campaign)
                .exclusionKeyword(exclusionKeyword)
                .build();
    }
    public Campaign getCampaign(String name){
        return Campaign.builder()
                .campaignId(1L)
                .camCampaignName(name)
                .build();
    }


}
