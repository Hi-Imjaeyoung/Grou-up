package growup.spring.springserver.exclusionKeyword.service;

import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.exception.exclusionKeyword.ExclusionKeyNotFound;
import growup.spring.springserver.exception.exclusionKeyword.ExistExclusionKeyword;
import growup.spring.springserver.exclusionKeyword.domain.ExclusionKeyword;
import growup.spring.springserver.exclusionKeyword.dto.ExclusionKeywordRequestDto;
import growup.spring.springserver.exclusionKeyword.dto.ExclusionKeywordResponseDto;
import growup.spring.springserver.exclusionKeyword.repository.ExclusionKeywordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.extractProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExclusionKeywordServiceTest {
    @InjectMocks
    private ExclusionKeywordService exclusionKeywordService;

    @Mock
    private ExclusionKeywordRepository exclusionKeywordRepository;
    @Mock
    private CampaignService campaignService;

    @DisplayName("addExclusionKeyword() : Error2. 캠패인을 못 찾을 시")
    @Test
    void test1_2(){
        //when
        doThrow(new CampaignNotFoundException()).when(campaignService).getMyCampaign(any(Long.class),any(String.class));
        final List<String> keywords = List.of("exclusionKeyword1");
        //given
        final Exception result = assertThrows(CampaignNotFoundException.class,
                ()->exclusionKeywordService.addExclusionKeyword(getExclusionRequestDto(keywords,1L),"Email")
        );
        //then
        assertThat(result.getMessage()).isEqualTo("현재 등록된 캠페인이 없습니다.");
    }

    @DisplayName("addExclusionKeyword() : Success")
    @Test
    void test1_3(){
        //when
        doReturn(getCampaign(1L,"campaign1")).when(campaignService).getMyCampaign(any(Long.class),any(String.class));
        doReturn(false).when(exclusionKeywordRepository).existsByExclusionKeyword("exclusionKeyword1");
        doReturn(true).when(exclusionKeywordRepository).existsByExclusionKeyword("exclusionKeyword2");
        doReturn(getExclusionKeyword(1L,"exclusionKeyword1",getCampaign(1L,"campaign1")))
                .when(exclusionKeywordRepository)
                .save(any(ExclusionKeyword.class));
        final List<String> keywords = List.of("exclusionKeyword1","exclusionKeyword2");
        //given
        final ExclusionKeywordResponseDto result = exclusionKeywordService.addExclusionKeyword(getExclusionRequestDto(keywords,1L),"Email");
        //then
        assertThat(result.getResponseData()).isEqualTo(1L);
        assertThat(result.getRequestData()).isEqualTo(2L);
    }

    @DisplayName("addExclusionKeyword() : Success 2. null 키워드가 넘어올시 실패 처리")
    @Test
    void test1_4(){
        //when
        doReturn(getCampaign(1L,"campaign1")).when(campaignService).getMyCampaign(any(Long.class),any(String.class));
        doReturn(false).when(exclusionKeywordRepository).existsByExclusionKeyword("exclusionKeyword1");
        doReturn(true).when(exclusionKeywordRepository).existsByExclusionKeyword("exclusionKeyword2");
        doReturn(getExclusionKeyword(1L,"exclusionKeyword1",getCampaign(1L,"campaign1")))
                .when(exclusionKeywordRepository)
                .save(any(ExclusionKeyword.class));
        final List<String> keywords = new ArrayList<>();
        keywords.add("exclusionKeyword1");
        keywords.add("exclusionKeyword2");
        keywords.add(null);
        //given
        final ExclusionKeywordResponseDto result = exclusionKeywordService.addExclusionKeyword(getExclusionRequestDto(keywords,1L),"Email");
        //then
        assertThat(result.getResponseData()).isEqualTo(1L);
        assertThat(result.getRequestData()).isEqualTo(3L);
    }

    @DisplayName("deleteExclusionKeyword() : Error1. 해당 제외키워드가 존재하지 않음")
    @Test
    void test2_1(){
        //when
        doReturn(getCampaign(1L,"campaign1")).when(campaignService).getMyCampaign(any(Long.class),any(String.class));
        doReturn(0).when(exclusionKeywordRepository).deleteByCampaign_campaignIdANDExclusionKeyword(any(Long.class),any(String.class));
        final List<String> keywords = List.of("exclusionKeyword1");
        //given
        final Exception result = assertThrows(ExclusionKeyNotFound.class,
                ()->exclusionKeywordService.deleteExclusionKeyword(getExclusionRequestDto(keywords,1L),"EMAIL")
        );
        //then
        assertThat(result.getMessage()).isEqualTo("해당 제외키워드가 없습니다");
    }

    @DisplayName("deleteExclusionKeyword() : Error2. 해당 캠패인이 없는 경우")
    @Test
    void test2_2(){
        //when
        doThrow(new CampaignNotFoundException()).when(campaignService).getMyCampaign(any(Long.class),any(String.class));
        final List<String> keywords = List.of("exclusionKeyword1");
        //given
        final Exception result = assertThrows(CampaignNotFoundException.class,
                ()->exclusionKeywordService.deleteExclusionKeyword(getExclusionRequestDto(keywords,1L),"EMAIL")
        );
        //then
        assertThat(result.getMessage()).isEqualTo("현재 등록된 캠페인이 없습니다.");
    }


    @DisplayName("deleteExclusionKeyword() : Success. 삭제 성공~")
    @Test
    void test2_3(){
        //when
        doReturn(getCampaign(1L,"campaign1")).when(campaignService).getMyCampaign(any(Long.class),any(String.class));
        doReturn(1).when(exclusionKeywordRepository).deleteByCampaign_campaignIdANDExclusionKeyword(any(Long.class),any(String.class));
        final List<String> keywords = List.of("exclusionKeyword1");
        //given
        final boolean result = exclusionKeywordService.deleteExclusionKeyword(getExclusionRequestDto(keywords,1L),"EMAIL");
        //then
        assertThat(result).isEqualTo(true);
    }

    @DisplayName("getExclusionKeywords(): Error1. 데이터가 존재하지 않음")
    @Test
    void test3_1(){
        //when
        doReturn(List.of()).when(exclusionKeywordRepository).findAllByCampaign_campaignId(any(Long.class));
        //given
        final Exception result = assertThrows(ExclusionKeyNotFound.class,
                () -> exclusionKeywordService.getExclusionKeywords(1L));
        //then
        assertThat(result.getMessage()).isEqualTo("해당 제외키워드가 없습니다");
    }
    @DisplayName("getExclusionKeywords(): Success")
    @Test
    void test3_2(){
        //when
        doReturn(List.of(getExclusionKeyword(1L,"exK1",getCampaign(1L,"cam1")),
                getExclusionKeyword(2L,"exK2",getCampaign(1L,"cam1")),
                getExclusionKeyword(3L,"exK3",getCampaign(1L,"cam1")))).when(exclusionKeywordRepository).findAllByCampaign_campaignId(any(Long.class));
        //given
        final List<ExclusionKeywordResponseDto> result = exclusionKeywordService.getExclusionKeywords(1L);
        //then
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(0).getExclusionKeyword()).isEqualTo("exK1");
        assertThat(result.get(1).getExclusionKeyword()).isEqualTo("exK2");
        assertThat(result.get(2).getExclusionKeyword()).isEqualTo("exK3");
        assertThat(result.get(0).getAddTime()).isEqualTo(LocalDate.now());
    }

    @DisplayName("saveNoExistExclusionKeyword() : 이미 해당 제외 키워드가 존재하는 경우")
    @Test
    @WithAuthUser
    void test4_1(){
        //W
        doReturn(true).when(exclusionKeywordRepository).existsByExclusionKeyword(any(String.class));
        //G
        final Exception result = assertThrows(ExistExclusionKeyword.class,()->
                exclusionKeywordService.saveNoExistExclusionKeyword("key",getCampaign(1L,"camp"))
                );
        //T
        assertThat(result.getMessage()).isEqualTo("이미 제외 키워드가 존재합니다");
    }

    @DisplayName("saveNoExistExclusionKeyword() : 제외 키워드 저장 성공")
    @Test
    @WithAuthUser
    void test4_2(){
        //W
        doReturn(false).when(exclusionKeywordRepository).existsByExclusionKeyword(any(String.class));
        //G
        exclusionKeywordService.saveNoExistExclusionKeyword("key",getCampaign(1L,"camp"));
        //T
        verify(exclusionKeywordRepository,times(1)).save(any());
    }


    public ExclusionKeyword getExclusionKeyword(Long id, String key, Campaign campaign) {
        return ExclusionKeyword.builder()
                .id(id)
                .exclusionKeyword(key)
                .insDate(LocalDateTime.now())
                .campaign(campaign)
                .build();
    }

    public Campaign getCampaign(Long id,String title){
        return Campaign.builder()
                .campaignId(id)
                .camCampaignName(title)
                .build();
    }

    public ExclusionKeywordRequestDto getExclusionRequestDto(List<String> key, Long campaignId){
        return ExclusionKeywordRequestDto.builder()
                .exclusionKeyword(key)
                .campaignId(campaignId)
                .build();
    }
}
