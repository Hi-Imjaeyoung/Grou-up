package growup.spring.springserver.keyword.service;

import growup.spring.springserver.exception.exclusionKeyword.ExclusionKeyNotFound;
import growup.spring.springserver.exception.keyword.CampaignKeywordNotFoundException;
import growup.spring.springserver.exclusionKeyword.dto.ExclusionKeywordResponseDto;
import growup.spring.springserver.exclusionKeyword.service.ExclusionKeywordService;
import growup.spring.springserver.keyword.domain.Keyword;
import growup.spring.springserver.keyword.repository.KeywordRepository;
import growup.spring.springserver.keyword.dto.KeywordResponseDto;
import growup.spring.springserver.keywordBid.dto.KeywordBidDto;
import growup.spring.springserver.keywordBid.dto.KeywordBidResponseDto;
import growup.spring.springserver.keywordBid.service.KeywordBidService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class KeywordServiceTest {
    @Mock
    private KeywordRepository keywordRepository;
    @Mock
    private ExclusionKeywordService exclusionKeywordService;
    @Mock
    private KeywordBidService keywordBidService;

    @InjectMocks
    private KeywordService keywordService;


    @Test
    @DisplayName("getKeyWords(): Success")
    void test1_7(){
        //when
        final LocalDate start = LocalDate.of(2024,12,14);
        final LocalDate end = LocalDate.of(2025,1,14);
        doReturn(List.of(getKeyword("exKeyAndBid",660.0,0.0,10L,123L,2L,11000.0)
                ,getKeyword("exKey",660.0,0.0,10L,123L,4L,0.0)
                ,getKeyword("bidKey",660.0,0.0,112L,123L,34L,0.0)
                ,getKeyword("nonKey",660.0,0.0,112L,123L,34L,0.0)))
                .when(keywordRepository).findAllByDateANDCampaign(any(LocalDate.class),any(LocalDate.class),any(Long.class));
        doReturn(List.of(
                getExclusionKeyword("exKeyAndBid",1L,LocalDate.now()),
                getExclusionKeyword("exKey",1L,LocalDate.now()))
        ).when(exclusionKeywordService).getExclusionKeywords(any(Long.class));
        doReturn(getKeywordBidResponseDto(2,2,
                List.of(getKeywordBidDto("exKeyAndBid",10L),
                        getKeywordBidDto("bidKey",10L)))
        ).when(keywordBidService).getKeywordBids(any(Long.class));
        //given
        final List<KeywordResponseDto> result  = keywordService.getKeywordsByCampaignId(start,end,1L);
        //then
        for(KeywordResponseDto dto : result){
            if(dto.getKeyKeyword().equals("exKeyAndBid")){
                assertThat(dto.getKeyBidFlag()).isTrue();
                assertThat(dto.getKeyExcludeFlag()).isTrue();
            }
            if(dto.getKeyKeyword().equals("bidKey")){
                assertThat(dto.getKeyBidFlag()).isTrue();
                assertThat(dto.getKeyExcludeFlag()).isFalse();
            }
            if(dto.getKeyKeyword().equals("exKey")){
                assertThat(dto.getKeyBidFlag()).isFalse();
                assertThat(dto.getKeyExcludeFlag()).isTrue();
            }
        }
    }

    //기존 method 에서 분리
    @Test
    @DisplayName("checkDateFormat(): Error 1. 조회 시작날과 끝의 순서가 이상할 때")
    void test2_1(){
        //when
        final LocalDate start = LocalDate.of(2024,12,14);
        final LocalDate end = LocalDate.of(2023,1,14);
        //given
        final boolean result  = keywordService.checkDateFormat(start,end);
        //then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("checkDateFormat(): Success")
    void test2_2(){
        //when
        final LocalDate start = LocalDate.of(2024,12,14);
        final LocalDate end = LocalDate.of(2025,1,14);
        //given
        final boolean result  = keywordService.checkDateFormat(start,end);
        //then
        assertThat(result).isTrue();
    }

    @DisplayName("getExclusionKeywordsToSet(): Error1. 제외키워드가 없을 경우")
    @Test
    void test3_1(){
        //when
        doThrow(ExclusionKeyNotFound.class)
                .when(exclusionKeywordService).getExclusionKeywords(1L);
        //given
        Set<String> result = keywordService.getExclusionKeywordToSet(1L);
        //then
        assertThat(result).hasSize(0);
    }

    @DisplayName("getExclusionKeywordsToSet(): Success")
    @Test
    void test3_2(){
        //when
        doReturn(List.of(
                getExclusionKeyword("exKey1",1L,LocalDate.now()),
                getExclusionKeyword("exKey2",1L,LocalDate.now()),
                getExclusionKeyword("exKey3",1L,LocalDate.now()))
        ).when(exclusionKeywordService).getExclusionKeywords(1L);
        //given
        Set<String> result = keywordService.getExclusionKeywordToSet(1L);
        //then
        assertThat(result).hasSize(3);
        assertThat(result.contains("exKey1")).isTrue();
        assertThat(result.contains("exKey4")).isFalse();
    }

    @Test
    @DisplayName("summeryKeywordData(): Error 1. 해당 데이터 없음")
    void test4_1(){
        //when
        List<Keyword> data = List.of();
        //given
        final CampaignKeywordNotFoundException result  = assertThrows(CampaignKeywordNotFoundException.class,
                ()-> keywordService.summeryKeywordData(data) );
        //then
        assertThat(result.getErrorCode().getMessage()).isEqualTo("해당 캠페인의 키워드가 없습니다.");
    }

    @Test
    @DisplayName("summeryKeywordData(): Error 1. Roas, Cvr 계산 시 분모가 0 일 경우?")
    void test4_2(){
        //when
        List<Keyword> data = List.of(getKeyword("keyword1",0.0,0.0,0L,0L,0L,0.0)
                ,getKeyword("keyword1",0.0,0.0,0L,0L,0L,0.0)
                ,getKeyword("keyword1",0.0,0.0,0L,0L,0L,0.0));
        //given
        final HashMap<String,KeywordResponseDto> result  = keywordService.summeryKeywordData(data);
        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get("keyword1").getKeyKeyword()).isEqualTo("keyword1");
        assertThat(result.get("keyword1").getKeyCvr()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("summeryKeywordData(): Success 1. keyClickRate Test")
    void test4_3(){
        //when
        List<Keyword> data = List.of(getKeyword("keyword1",660.0,0.0,10L,123L,0L,11000.0)
                ,getKeyword("keyword1",0.0,0.0,10L,123L,0L,0.0)
                ,getKeyword("keyword1",0.0,0.0,10L,123L,0L,0.0));
        //given
        final HashMap<String,KeywordResponseDto> result  = keywordService.summeryKeywordData(data);
        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get("keyword1").getKeyClicks()).isEqualTo(30);
        assertThat(result.get("keyword1").getKeyImpressions()).isEqualTo(369);
        assertThat(result.get("keyword1").getKeyClickRate()).isEqualTo(((Math.round(((double)30/(double) 369)*10000))/100.0));
    }

    @Test
    @DisplayName("summeryKeywordData(): Success 2. cvr Test")
    void test4_4(){
        //when
        List<Keyword> data = List.of(getKeyword("keyword1",660.0,0.0,10L,123L,2L,11000.0)
                ,getKeyword("keyword1",0.0,0.0,10L,123L,4L,0.0)
                ,getKeyword("keyword1",0.0,0.0,112L,123L,34L,0.0));
        //given
        final HashMap<String,KeywordResponseDto> result  = keywordService.summeryKeywordData(data);
        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get("keyword1").getKeyClicks()).isEqualTo(132);
        assertThat(result.get("keyword1").getKeyTotalSales()).isEqualTo(40);
        assertThat(result.get("keyword1").getKeyCvr()).isEqualTo(((Math.round(((double)40/(double) 132)*10000))/100.0));
    }

    @Test
    @DisplayName("summeryKeywordData(): Success 3. cpc Test")
    void test4_5(){
        //when
        List<Keyword> data = List.of(getKeyword("keyword1",660.0,0.0,10L,123L,2L,11000.0)
                ,getKeyword("keyword1",660.0,0.0,10L,123L,4L,0.0)
                ,getKeyword("keyword1",660.0,0.0,112L,123L,34L,0.0));
        //given
        final HashMap<String,KeywordResponseDto> result  = keywordService.summeryKeywordData(data);
        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get("keyword1").getKeyClicks()).isEqualTo(132);
        assertThat(result.get("keyword1").getKeyAdcost()).isEqualTo(1980);
        assertThat(result.get("keyword1").getKeyCpc()).isEqualTo(((Math.round(((double)1980/(double) 132)*100))/100.0));
    }

    @DisplayName("checkKeyTypeExclusion() : Success")
    @Test
    void test5_1(){
        //when
        HashMap<String,KeywordResponseDto> map = new HashMap<>();
        Set<String> exclusionKeys = new HashSet<>();
        map.put("k1",KeywordResponseDto.builder().build());
        map.put("k2",KeywordResponseDto.builder().build());
        map.put("k3",KeywordResponseDto.builder().build());
        map.put("k4",KeywordResponseDto.builder().build());
        exclusionKeys.add("k2");
        //given
        final List<KeywordResponseDto> result = keywordService.checkKeyTypeExclusion(map,exclusionKeys);
        //then
        assertThat(result.get(1).getKeyExcludeFlag()).isEqualTo(true);
    }

    @DisplayName("addBids() : Success")
    @Test
    void test6_1(){
        //when
        HashMap<String,KeywordResponseDto> map = new HashMap<>();
        map.put("k1",KeywordResponseDto.builder().build());
        map.put("k2",KeywordResponseDto.builder().build());
        map.put("k3",KeywordResponseDto.builder().build());
        map.put("k4",KeywordResponseDto.builder().build());
        List<KeywordBidDto> keywordBidDtos = List.of(getKeywordBidDto("k1",1L),
                getKeywordBidDto("k2",2L),
                getKeywordBidDto("k3",3L),
                getKeywordBidDto("k4",4L));
        //given
        final List<KeywordResponseDto> result = keywordService.addBids(map,keywordBidDtos);
        //then
        assertThat(result.get(0).getBid()).isEqualTo(1L);
        assertThat(result.get(1).getBid()).isEqualTo(2L);
        assertThat(result.get(2).getBid()).isEqualTo(3L);
        assertThat(result.get(3).getBid()).isEqualTo(4L);
    }

    @DisplayName("getKeywordsByDateAndCampaignIdAndKeys() : Success")
    @Test
    void test7_1(){
        //when
        doReturn(List.of(getKeyword("k1",0.0,0.0,0L,0L,0L,0.0)
                        ,getKeyword("k2",0.0,0.0,0L,0L,0L,0.0)
                        ,getKeyword("k3",0.0,0.0,0L,0L,0L,0.0)))
                .when(keywordRepository).findKeywordsByDateAndCampaignIdAndKeys(any(LocalDate.class),any(LocalDate.class),any(Long.class),any(List.class));
        final LocalDate start = LocalDate.of(2024,12,14);
        final LocalDate end = LocalDate.of(2025,1,14);
        List<KeywordBidDto> keywordBidDtos = List.of(getKeywordBidDto("k1",1L),
                getKeywordBidDto("k2",2L),
                getKeywordBidDto("k3",3L));
        //given
        List<KeywordResponseDto> result = keywordService.getKeywordsByDateAndCampaignIdAndKeys(start,end,1L,keywordBidDtos);
        //then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getBid()).isEqualTo(1L);
        assertThat(result.get(1).getBid()).isEqualTo(2L);
        assertThat(result.get(2).getBid()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getBidKeywordToSet() : Error 1. 빈 리스트를 받아 올 떄")
    void test8_1(){
        //when
        doReturn(getKeywordBidResponseDto(0,0,List.of())).when(keywordBidService).getKeywordBids(any(Long.class));
        //given
        final Set<String> results = keywordService.getBidKeywrodToSet(1L);
        //then
        assertThat(results).hasSize(0);
    }

    @Test
    @DisplayName("getBidKeywordToSet() : Success")
    void test8_2(){
        //when
        doReturn(getKeywordBidResponseDto(4,4,
                List.of(getKeywordBidDto("overlapKey",10L),
                        getKeywordBidDto("overlapKey",10L),
                        getKeywordBidDto("test1",10L),
                        getKeywordBidDto("test2",10L))
                )).when(keywordBidService).getKeywordBids(any(Long.class));
        //given
        final Set<String> results = keywordService.getBidKeywrodToSet(1L);
        //then
        assertThat(results).hasSize(3);
        assertThat(results.contains("test1")).isTrue();
        assertThat(results.contains("test3")).isFalse();
    }

    @Test
    @DisplayName("checkKeyTypeKeywordBid() : Success")
    void test9_1(){
        //when
        List<KeywordResponseDto> list = List.of(KeywordResponseDto.builder().keyKeyword("k1").keyBidFlag(false).build(),
                                                KeywordResponseDto.builder().keyKeyword("k2").keyBidFlag(false).build(),
                                                KeywordResponseDto.builder().keyKeyword("k3").keyBidFlag(false).build(),
                                                KeywordResponseDto.builder().keyKeyword("k4").keyBidFlag(false).build());
        Set<String> bidKey = new HashSet<>();
        bidKey.add("k2");
        //given
        keywordService.checkKeyTypeKeywordBid(list,bidKey);
        //then
        assertThat(list.get(1).getKeyBidFlag()).isEqualTo(true);
        assertThat(list.get(2).getKeyBidFlag()).isEqualTo(false);
    }

    @Test
    @DisplayName("summeryKeySalesOption() : error 1. map Is Empty")
    void test10_1(){
        //when
        Map<String,Long> originData = new HashMap<>();
        Map<String,Long> inputData = new HashMap<>();
        inputData.put("옵션1",3L);
        inputData.put("옵션2",3L);
        //given
        keywordService.summeryKeySalesOption(originData,inputData);
        //then
        assertThat(originData).hasSize(2);
        assertThat(originData.get("옵션1")).isEqualTo(3L);
        assertThat(originData.get("옵션2")).isEqualTo(3L);
    }

    @Test
    @DisplayName("summeryKeySalesOption() : Success")
    void test10_2(){
        //when
        Map<String,Long> originData = new HashMap<>();
        Map<String,Long> inputData = new HashMap<>();
        originData.put("옵션1",1L);
        originData.put("옵션2",1L);
        originData.put("옵션3",1L);

        inputData.put("옵션1",3L);
        inputData.put("옵션5",3L);
        //given
        keywordService.summeryKeySalesOption(originData,inputData);
        //then
        assertThat(originData).hasSize(4);
        assertThat(originData.get("옵션1")).isEqualTo(4L);
        assertThat(originData.get("옵션5")).isEqualTo(3L);
    }

    public ExclusionKeywordResponseDto getExclusionKeyword(String key,
                                                           Long campaignId,
                                                           LocalDate localDate){
        return ExclusionKeywordResponseDto.builder()
                .exclusionKeyword(key)
                .campaignId(campaignId)
                .addTime(localDate)
                .build();
    }

    public Keyword getKeyword(String title,
                              Double adCost,
                              Double cpc,
                              Long clicks,
                              Long impression,
                              Long totalSale,
                              Double adSale){
        return Keyword.builder()
                .keyCvr(0.0)
                .keyClickRate(0.0)
                .keyRoas(0.0)
                .keyDate(LocalDate.of(2024,11,1))
                .keyKeyword(title)
                .keyAdcost(adCost)
                .keyCpc(cpc)
                .keyClicks(clicks)
                .keyImpressions(impression)
                .keyTotalSales(totalSale)
                .keyAdsales(adSale)
                .build();
    }
    public static KeywordBidDto getKeywordBidDto(String keyword, Long bid){
        return KeywordBidDto.builder()
                .keyword(keyword)
                .bid(bid)
                .build();
    }

    public static KeywordBidResponseDto getKeywordBidResponseDto(int request,
                                                                 int response,
                                                                 List<KeywordBidDto> data){
        return KeywordBidResponseDto.builder()
                .responseNumber(response)
                .requestNumber(request)
                .response(data)
                .build();
    }


}
