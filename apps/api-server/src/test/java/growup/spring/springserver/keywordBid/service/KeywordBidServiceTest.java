package growup.spring.springserver.keywordBid.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.keywordBid.KeywordBidNotFound;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.keywordBid.domain.KeywordBid;
import growup.spring.springserver.keywordBid.dto.KeywordBidDto;
import growup.spring.springserver.keywordBid.dto.KeywordBidRequestDtos;
import growup.spring.springserver.keywordBid.dto.KeywordBidResponseDto;
import growup.spring.springserver.keywordBid.repository.KeywordBidRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KeywordBidServiceTest {
    @InjectMocks
    private KeywordBidService keywordBidService;

    @Mock
    private KeywordBidRepository keywordBidRepository;

    @DisplayName("addKeywordBids():error 1. 빈 리스트를 저장할 떄")
    @Test
    void test1_1(){
        //when
        //Controller 단에서 CampaignService를 호출하기 떄문에 아래 코드는 의미가 없어진다.
//        doReturn(getCampaign()).when(campaignService).getMyCampaign(1L,"test@test.com");
        doReturn(true).when(keywordBidRepository).existsByCampaign_CampaignIdAndKeyword(1L,"passKey1");
        final KeywordBidRequestDtos keywordBidRequestDtos = getKeywordBidRequestDtos(1L, List.of(getKeywordBidRequestDto("passKey1",1000L)));
        //given
        final KeywordBidResponseDto result = keywordBidService.addKeywordBids(keywordBidRequestDtos,getCampaign());
        //then
        assertThat(result.getRequestNumber()).isEqualTo(1);
        assertThat(result.getResponseNumber()).isEqualTo(0);
    }

    @DisplayName("addKeywordBids(): Success")
    @Test
    void test1_2(){
        //when
//        doReturn(getCampaign()).when(campaignService).getMyCampaign(1L,"test@test.com");
        doReturn(false).when(keywordBidRepository).existsByCampaign_CampaignIdAndKeyword(1L,"passKey1");
        final KeywordBidRequestDtos keywordBidRequestDtos = getKeywordBidRequestDtos(1L, List.of(getKeywordBidRequestDto("passKey1",1000L)));
        //given
        final KeywordBidResponseDto result = keywordBidService.addKeywordBids(keywordBidRequestDtos,getCampaign());
        //then
        assertThat(result.getRequestNumber()).isEqualTo(1);
        assertThat(result.getResponseNumber()).isEqualTo(1);
    }

    @DisplayName("checkOverlapKeywordBid(): Success. 이미 저장되있는 객체 구분")
    @Test
    void test2_1(){
        //when
        doReturn(false).when(keywordBidRepository).existsByCampaign_CampaignIdAndKeyword(1L,"passKey");
        doReturn(false).when(keywordBidRepository).existsByCampaign_CampaignIdAndKeyword(1L,"passKey1");
        doReturn(true).when(keywordBidRepository).existsByCampaign_CampaignIdAndKeyword(1L,"overlapKey");
        List<KeywordBidDto> list = List.of(getKeywordBidRequestDto("passKey",100L),
                        getKeywordBidRequestDto("passKey1",1000L),
                        getKeywordBidRequestDto("overlapKey",1000L));
        //given
        final List<KeywordBid> result = keywordBidService.checkOverlapKeywordBid(list,getCampaign());
        //then
        assertThat(result).hasSize(2);
    }

    @DisplayName("getKeywordBids() : Success 1. 빈 리스트")
    @Test
    void test3_1(){
        //when
        doReturn(List.of()).when(keywordBidRepository).findAllByCampaign_CampaignId(1L);
        //given
        final KeywordBidResponseDto result = keywordBidService.getKeywordBids(1L);
        //then
        assertThat(result.getResponse()).hasSize(0);
    }

    @DisplayName("getKeywordBids() : Success 2.")
    @Test
    void test3_2(){
        //when
        doReturn(List.of(getKeywordBid("test1",1000L,getCampaign()),
                        getKeywordBid("test1",1000L,getCampaign()),
                        getKeywordBid("test1",1000L,getCampaign())))
                .when(keywordBidRepository).findAllByCampaign_CampaignId(1L);
        //given
        final KeywordBidResponseDto result = keywordBidService.getKeywordBids(1L);
        //then
        assertThat(result.getResponse()).hasSize(3);
    }

    @DisplayName("updateKeywordBids():Success 1. 다중의 요청이 전부 성공")
    @Test
    void test4_1(){
        //when
        doReturn(Optional.of(getKeywordBid("passKey1",500L,getCampaign())))
                .when(keywordBidRepository).findByCampaign_CampaignIdANDKeyword(1L,"passKey1");
        doReturn(Optional.of(getKeywordBid("passKey2",500L,getCampaign())))
                .when(keywordBidRepository).findByCampaign_CampaignIdANDKeyword(1L,"passKey2");
        final KeywordBidRequestDtos keywordBidRequestDtos =
                getKeywordBidRequestDtos(1L,
                        List.of(getKeywordBidRequestDto("passKey1",1000L)
                                , getKeywordBidRequestDto("passKey2",1500L)
                        )
                );
        //given
        final KeywordBidResponseDto result = keywordBidService.updateKeywordBids(keywordBidRequestDtos);
        //then
        assertThat(result.getResponse()).hasSize(2);
        assertThat(result.getRequestNumber()).isEqualTo(2);
        assertThat(result.getResponseNumber()).isEqualTo(2);
        assertThat(result.getResponse().get(0).getBid()).isEqualTo(1000L);
        assertThat(result.getResponse().get(1).getBid()).isEqualTo(1500L);
    }

    @DisplayName("updateKeywordBids():Success 2. 다중의 요청 중 일부 성공")
    @Test
    void test4_2(){
        //when
        doReturn(Optional.empty())
                .when(keywordBidRepository).findByCampaign_CampaignIdANDKeyword(1L,"passKey1");
        doReturn(Optional.of(getKeywordBid("passKey2",500L,getCampaign())))
                .when(keywordBidRepository).findByCampaign_CampaignIdANDKeyword(1L,"passKey2");
        final KeywordBidRequestDtos keywordBidRequestDtos =
                getKeywordBidRequestDtos(1L,
                        List.of(getKeywordBidRequestDto("passKey1",1000L)
                                , getKeywordBidRequestDto("passKey2",1500L)
                        )
                );
        //given
        final KeywordBidResponseDto result = keywordBidService.updateKeywordBids(keywordBidRequestDtos);
        //then
        assertThat(result.getResponse()).hasSize(1);
        assertThat(result.getRequestNumber()).isEqualTo(2);
        assertThat(result.getResponseNumber()).isEqualTo(1);
    }

    @DisplayName("findKeywordBidByCampaignIDAndKey() : Error. 해당 입찰가 정보없음")
    @Test
    void test5_2(){
        //when
        doReturn(Optional.empty())
                .when(keywordBidRepository).findByCampaign_CampaignIdANDKeyword(1L,"testKey1");
        //given
        final KeywordBidNotFound result = assertThrows(KeywordBidNotFound.class,
                ()->keywordBidService.findKeywordBidByCampaignIDAndKey(1L,"testKey1"));
        //then
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.KEYWORDBID_NOT_FOUND);
    }

    @DisplayName("findKeywordBidByCampaignIDAndKey() : Success")
    @Test
    void test5_1(){
        //when
        doReturn(Optional.of(getKeywordBid("testKey1",1000L,getCampaign())))
                .when(keywordBidRepository).findByCampaign_CampaignIdANDKeyword(1L,"testKey1");
        //given
        final KeywordBid result = keywordBidService.findKeywordBidByCampaignIDAndKey(1L,"testKey1");
        //then
        assertThat(result.getBid()).isEqualTo(1000L);
        assertThat(result.getKeyword()).isEqualTo("testKey1");
    }

    @DisplayName("deleteKeywordBidByCampaignIdAndKey() : Success 1.다중 요청 시 일부 성공")
    @Test
    void test6_1(){
        //when
        doReturn(1)
                .when(keywordBidRepository).deleteByCampaign_CampaignIdANDKeyword(1L,"key1");
        doReturn(0)
                .when(keywordBidRepository).deleteByCampaign_CampaignIdANDKeyword(1L,"key2");
        final KeywordBidRequestDtos keywordBidRequestDtos =
                getKeywordBidRequestDtos(1L,
                        List.of(getKeywordBidRequestDto("key1",1000L)
                                , getKeywordBidRequestDto("key2",1500L)
                        )
                );
        //given
        final KeywordBidResponseDto result = keywordBidService.deleteByCampaignIdAndKeyword(keywordBidRequestDtos);
        //then
        assertThat(result.getRequestNumber()).isEqualTo(2);
        assertThat(result.getResponseNumber()).isEqualTo(1);
    }

    @DisplayName("deleteKeywordBidByCampaignIdAndKey() : Success 2.다중 요청 전부 성공")
    @Test
    void test6_2(){
        //when
        doReturn(1)
                .when(keywordBidRepository).deleteByCampaign_CampaignIdANDKeyword(1L,"key1");
        doReturn(1)
                .when(keywordBidRepository).deleteByCampaign_CampaignIdANDKeyword(1L,"key2");
        final KeywordBidRequestDtos keywordBidRequestDtos =
                getKeywordBidRequestDtos(1L,
                        List.of(getKeywordBidRequestDto("key1",1000L)
                                , getKeywordBidRequestDto("key2",1500L)
                        )
                );
        //given
        final KeywordBidResponseDto result = keywordBidService.deleteByCampaignIdAndKeyword(keywordBidRequestDtos);
        //then
        assertThat(result.getRequestNumber()).isEqualTo(2);
        assertThat(result.getResponseNumber()).isEqualTo(2);
    }

    public static Campaign getCampaign(){
        return Campaign.builder().campaignId(1L).camCampaignName("testCamp").build();
    }
    public static KeywordBid getKeywordBid(String key,Long bid,Campaign campaign){
        return KeywordBid.builder()
                .bid(bid)
                .keyword(key)
                .campaign(campaign)
                .build();
    }
    public static KeywordBidRequestDtos getKeywordBidRequestDtos(Long campaignId,List<KeywordBidDto> list){
        return KeywordBidRequestDtos.builder()
                .campaignId(campaignId)
                .data(list)
                .build();
    }
    public static KeywordBidDto getKeywordBidRequestDto(String keyword,Long bid){
        return KeywordBidDto.builder()
                .keyword(keyword)
                .bid(bid)
                .build();
    }
}
