package growup.spring.springserver.campaign.service;

import growup.spring.springserver.campaign.TypeChangeCampaign;
import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.dto.CampaignResponseDto;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import growup.spring.springserver.keyword.service.KeywordService;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {
    @InjectMocks
    private CampaignService campaignService;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private KeywordService keywordService;

    @Test
    @DisplayName("getMyCampaigns(): ErrorCase1.캠패인 목록이 없을 때")
    void test1(){
        //given
        doReturn(new ArrayList<Campaign>()).when(campaignRepository).findAllByMember(any(Member.class));
        //when
        final Exception result = assertThrows(GrouException.class,
                ()->campaignService.getCampaignsByMember(getMember()));
        //then
        assertThat(result.getMessage()).isEqualTo("현재 등록된 캠페인이 없습니다.");
    }

    @Test
    @DisplayName("getMyCampaigns(): Success")
    void test3(){
        //given
        doReturn(List.of(
                    Campaign.builder().camCampaignName("campaign1").campaignId(1L).build(),
                    Campaign.builder().camCampaignName("campaign2").campaignId(2L).build(),
                    Campaign.builder().camCampaignName("campaign3").campaignId(3L).build())
        ).when(campaignRepository).findAllByMember(any(Member.class));
        //when
        List<CampaignResponseDto> result = campaignService.getCampaignsByMember(getMember()).stream().map(TypeChangeCampaign::entityToResponseDto).toList();
        //then
        //TODO: 한 번에 테스트 할 수는 없을까?
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(0).getTitle()).isEqualTo("campaign1");
        assertThat(result.get(1).getTitle()).isEqualTo("campaign2");
        assertThat(result.get(2).getTitle()).isEqualTo("campaign3");
        assertThat(result.get(0).getCampaignId()).isEqualTo(1L);
        assertThat(result.get(1).getCampaignId()).isEqualTo(2L);
        assertThat(result.get(2).getCampaignId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getMyCampaign() : Error 캠패인 단건 조회 실패")
    void test4_1(){
        //given
        doReturn(Optional.empty()).when(campaignRepository).findByCampaignIdANDEmail(1L,"test@test.com");
        //when
        GrouException result = assertThrows(GrouException.class,
                ()-> campaignService.getMyCampaign(1L,"test@test.com"));
        //then
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.CAMPAIGN_NOT_FOUND);
    }

    @Test
    @DisplayName("getMyCampaign() : success 캠패인 단건 조회")
    void test4(){
        //given
        doReturn(Optional.of(getCampaign())).when(campaignRepository).findByCampaignIdANDEmail(1L,"test@test.com");
        //when
        Campaign result = campaignService.getMyCampaign(1L,"test@test.com");
        //then
        assertThat(result.getCampaignId()).isEqualTo(1L);
    }
    public static Campaign getCampaign(){
        return Campaign.builder().campaignId(1L).camCampaignName("testCamp").build();
    }

    @Test
    @DisplayName("deleteCampaign() : 부분 성공")
    void deleteCampaign() {
        //given
        doReturn(Optional.empty()).when(campaignRepository).findByCampaignId(1L);
        doReturn(Optional.of(getCampaign())).when(campaignRepository).findByCampaignId(2L);
        doThrow(ConstraintViolationException.class).when(campaignRepository).findByCampaignId(3L);
        //when
        final int result = campaignService.deleteCampaign(List.of(1L,2L,3L));
        //then
        assertThat(result).isEqualTo(1);
    }


    public Member getMember(){
        return Member.builder()
                .email("test@test.com")
                .build();
    }

}
