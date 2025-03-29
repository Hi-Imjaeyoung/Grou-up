package growup.spring.springserver.keyword.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.keyword.domain.Keyword;
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
public class KeywordRepositoryTest{
    @Autowired
    private KeywordRepository keywordRepository;
    @Autowired
    private CampaignRepository campaignRepository;

    private Campaign campaign;
    private LocalDate start;
    private LocalDate end ;
    @BeforeEach
    void setUp(){
        campaign = Campaign.builder()
                .campaignId(1L)
                .camCampaignName("testCampaign")
                .build();
        campaignRepository.save(campaign);
        start = LocalDate.parse("2024-12-23",DateTimeFormatter.ISO_DATE);
        end = LocalDate.parse("2025-01-05",DateTimeFormatter.ISO_DATE);
    }

    @DisplayName("findAllByDateANDFlag() : Success 1. No Campaign Id")
    @Test
    void test3(){
        //when
        LocalDate dataDate = LocalDate.parse("2024-12-25",DateTimeFormatter.ISO_DATE);
        keywordRepository.save(getKeyword("title1",campaign,dataDate,false));
        keywordRepository.save(getKeyword("title1",campaign,dataDate,false));
        //given
        List<Keyword> result = keywordRepository.findAllByDateANDCampaign(start,end,2L);
        //then
        assertThat(result.size()).isEqualTo(0);
    }

    @DisplayName("findAllByDateANDFlag() : Success 2. About Date ")
    @Test
    void test6(){
        //when
        LocalDate inDataDate = LocalDate.parse("2024-12-25",DateTimeFormatter.ISO_DATE);
        LocalDate outDataDate = LocalDate.parse("2026-12-25",DateTimeFormatter.ISO_DATE);
        keywordRepository.save(getKeyword("title1",campaign,inDataDate,false));
        keywordRepository.save(getKeyword("title1",campaign,inDataDate,false));
        keywordRepository.save(getKeyword("title1",campaign,inDataDate,false));
        keywordRepository.save(getKeyword("title1",campaign,outDataDate,false));
        keywordRepository.save(getKeyword("title1",campaign,outDataDate,false));
        //given
        List<Keyword> result = keywordRepository.findAllByDateANDCampaign(start,end,1L);
        //then
        assertThat(result.size()).isEqualTo(3);
    }

    @DisplayName("findKeywordsByDateAndCampaignIdAndKeys(): Success")
    @Test
    void test7(){
        //when
        LocalDate inDataDate = LocalDate.parse("2024-12-25",DateTimeFormatter.ISO_DATE);
        LocalDate inDataDate2 = LocalDate.parse("2024-12-26",DateTimeFormatter.ISO_DATE);
        LocalDate outDataDate = LocalDate.parse("2026-12-25",DateTimeFormatter.ISO_DATE);
        keywordRepository.save(getKeyword("title1",campaign,inDataDate,false));
        keywordRepository.save(getKeyword("title1",campaign,inDataDate2,false));
        keywordRepository.save(getKeyword("title1",campaign,outDataDate,false));
        keywordRepository.save(getKeyword("title2",campaign,inDataDate,false));
        keywordRepository.save(getKeyword("title3",campaign,inDataDate,false));
        keywordRepository.save(getKeyword("title4",campaign,outDataDate,false));
        List<String> keys = List.of("title1","title2","title4");
        //given
        List<Keyword> result = keywordRepository.findKeywordsByDateAndCampaignIdAndKeys(start,end,1L,keys);
        //then
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("Keword의 salesOption이 null인 경우 JPA에서 빈 객체가 나올까? null 이 나올까?")
    void test8(){
        //when
        LocalDate dataDate = LocalDate.parse("2024-12-25",DateTimeFormatter.ISO_DATE);
        Keyword input = keywordRepository.save(getKeyword("title1",campaign,dataDate,false));
        //given
        final Keyword result = keywordRepository.findById(input.getId()).get();
        //then
        assertThat(result.getKeyKeyword()).isEqualTo("title1");
        assertThat(result.getKeyProductSales()).isNull();
    }

    @Test
    @DisplayName("deleteByCampaignIdAndDate()")
    @Transactional
    void test9(){
        //when
        LocalDate start = LocalDate.parse("2025-03-01",DateTimeFormatter.ISO_DATE);
        LocalDate end = LocalDate.parse("2025-03-29",DateTimeFormatter.ISO_DATE);
        LocalDate includeDate = LocalDate.parse("2025-03-20",DateTimeFormatter.ISO_DATE);
        LocalDate  excludeDate = LocalDate.parse("2025-04-01",DateTimeFormatter.ISO_DATE);
        keywordRepository.save(getKeyword("key1",campaign,includeDate,false));
        keywordRepository.save(getKeyword("key2",campaign,excludeDate,false));
        //given
        final int result = keywordRepository.deleteByCampaignIdAndDate(start,end,campaign.getCampaignId());
        assertThat(result).isEqualTo(1);
    }

    public Keyword getKeyword(String title, Campaign campaign){
        return Keyword.builder()
                .keyKeyword(title)
                .campaign(campaign)
                .build();
    }
    public Keyword getKeyword(String title, Campaign campaign,LocalDate localDate, boolean flag){
        return Keyword.builder()
                .keyDate(localDate)
                .keyKeyword(title)
                .campaign(campaign)
                .keyExcludeFlag(flag)
                .build();
    }
}
