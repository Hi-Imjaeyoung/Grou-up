package growup.spring.springserver.keywordBid.controller;

import com.nimbusds.jose.shaded.gson.*;
import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.global.InvalidDateFormatException;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.exception.global.RequestException;
import growup.spring.springserver.global.config.GsonConfig;
import growup.spring.springserver.global.config.JwtTokenProvider;
import growup.spring.springserver.keyword.dto.KeywordResponseDto;
import growup.spring.springserver.keyword.service.KeywordService;
import growup.spring.springserver.keywordBid.KeywordBidController;
import growup.spring.springserver.keywordBid.dto.KeywordBidDto;
import growup.spring.springserver.keywordBid.dto.KeywordBidRequestDtos;
import growup.spring.springserver.keywordBid.dto.KeywordBidResponseDto;
import growup.spring.springserver.keywordBid.service.KeywordBidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import static growup.spring.springserver.keywordBid.service.KeywordBidServiceTest.getCampaign;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(KeywordBidController.class)
public class KeywordBidControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeywordBidService keywordBidService;
    @MockBean
    private CampaignService campaignService;
    @MockBean
    private KeywordService keywordService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private Gson gson;

    @BeforeEach
    void set(){
         gson = GsonConfig.getGson();
    }

    @WithAuthUser
    @DisplayName("/adds: Error 1. body 누락")
    @ParameterizedTest
    @MethodSource("addsUrlIncorrectBodies")
    void test1(KeywordBidRequestDtos body) throws Exception {
        final String url = "/api/bid/adds";
        ResultActions resultActions = mockMvc.perform(post(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        resultActions.andExpectAll(
                status().isBadRequest()
        );
    }
    private static Stream<Arguments> addsUrlIncorrectBodies(){
        return Stream.of(
                Arguments.of(KeywordBidRequestDtos.builder()
                        .campaignId(1L)
                        .data(List.of())
                        .build()
                ),
                Arguments.of(KeywordBidRequestDtos.builder()
                        .campaignId(null)
                        .data(List.of(KeywordBidDto.builder().build()))
                        .build())
        );
    }

    @WithAuthUser
    @DisplayName("/adds: Error 2. CampaignNotFound 예외 처리")
    @Test
    void test1_2() throws Exception {
        doThrow(new CampaignNotFoundException()).when(campaignService).getMyCampaign(any(Long.class),any(String.class));
        final String url = "/api/bid/adds";
        final KeywordBidRequestDtos body = getRequest(1L,List.of(getKeywordBidDto(100L,"test")));
        ResultActions result = mockMvc.perform(post(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        result.andExpectAll(
                status().isBadRequest(),
                jsonPath("errorMessage").value("현재 등록된 캠페인이 없습니다.")
        );
    }

    @WithAuthUser
    @DisplayName("/adds : Success")
    @Test
    void test1_3() throws Exception {
        doReturn(getCampaign()).when(campaignService).getMyCampaign(any(Long.class),any(String.class));
        doReturn(getKeywordBidResponseDto(3,2, List.of(getKeywordBidDto(1L,"k"),getKeywordBidDto(2L,"2k"))))
                .when(keywordBidService)
                .addKeywordBids(any(KeywordBidRequestDtos.class),any(Campaign.class));
        final String url = "/api/bid/adds";
        final KeywordBidRequestDtos body = getRequest(1L,List.of(getKeywordBidDto(1L,"k"),getKeywordBidDto(2L,"2k"),getKeywordBidDto(2L,null)));
        ResultActions result = mockMvc.perform(post(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        result.andExpectAll(
                status().isOk(),
                jsonPath("data.requestNumber").value("3"),
                jsonPath("data.responseNumber").value("2"),
                jsonPath("data.response[0].keyword").value("k")
        );
    }

    // 이 검증이 의미가 있을지 고민입니다.
    @WithAuthUser
    @DisplayName("/gets : Errors 1. 파라미터 날짜 관련 예외들")
    @ParameterizedTest
    @MethodSource("getsIncorrectParam")
    void test2_1(String start, String end, String campaignId) throws Exception {
        doReturn(getKeywordBidResponseDto(1,1,List.of())).when(keywordBidService).getKeywordBids(any());
        doThrow(new RequestException()).when(keywordService).getKeywordsByDateAndCampaignIdAndKeys(any(),any(),any(),any());
        final String url = "/api/bid/gets";
        final ResultActions result = mockMvc.perform(get(url)
                .param("start",start)
                .param("end",end)
                .param("campaignId",campaignId));
        result.andExpectAll(
                status().isOk(),
                jsonPath("message").value("요청 날짜 형식 오류로 인한 빈 값 리턴.")
        );
    }
    public static Stream<Arguments> getsIncorrectParam(){
        return Stream.of(
                //LocalDate 형식 오류
                Arguments.of("2014-11","2015-11-01","1"),
                // 시작 끝 관계 오류
                Arguments.of("2024-11-01","2023-11-01","1")
        );
    }

    @WithAuthUser
    @DisplayName("/gets: Success")
    @Test
    void test2_2() throws Exception {
        doReturn(getKeywordBidResponseDto(1,1,List.of())).when(keywordBidService).getKeywordBids(any());
        doReturn(List.of(getKeywordResDto())).when(keywordService).getKeywordsByDateAndCampaignIdAndKeys(any(),any(),any(),any());
        final String url = "/api/bid/gets";
        final ResultActions result = mockMvc.perform(get(url)
                .param("start","2024-11-01")
                .param("end","2024-12-01")
                .param("campaignId","1"));
        result.andExpectAll(
                status().isOk(),
                jsonPath("data[0].bid").value("100"),
                jsonPath("data[0].keyKeyword").value("keyword")
        ).andDo(print());
    }


    @WithAuthUser
    @DisplayName("/update : Error 1. body 데이터 누락")
    @ParameterizedTest
    @MethodSource("addsUrlIncorrectBodies")
    void test3_1(KeywordBidRequestDtos body) throws Exception {
        final String url = "/api/bid/update";
        final ResultActions result = mockMvc.perform(patch(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        result.andExpectAll(
                status().isBadRequest()
        );
    }

    @WithAuthUser
    @DisplayName("/update : success")
    @Test
    void test3_2() throws Exception {
        doReturn(
                getKeywordBidResponseDto(2,1,List.of(getKeywordBidDto(200L,"k2")))
        ).when(keywordBidService).updateKeywordBids(any(KeywordBidRequestDtos.class));
        final String url = "/api/bid/update";
        final KeywordBidRequestDtos body =
                getRequest(1L, List.of(getKeywordBidDto(100L,"k1"),getKeywordBidDto(200L,"k2")));
        final ResultActions result = mockMvc.perform(patch(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        result.andExpectAll(
                status().isOk(),
                jsonPath("data.requestNumber").value("2"),
                jsonPath("data.responseNumber").value("1"),
                jsonPath("data.response[0].keyword").value("k2")
        );
    }

    @WithAuthUser
    @DisplayName("/delete : body 데이터 누락")
    @ParameterizedTest
    @MethodSource("addsUrlIncorrectBodies")
    void test4_1(KeywordBidRequestDtos body) throws Exception {
        final String url = "/api/bid/delete";
        final ResultActions result = mockMvc.perform(delete(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        result.andExpectAll(
                status().isBadRequest()
        );
    }

    @WithAuthUser
    @DisplayName("/delete : success")
    @Test
    void test4_2() throws Exception {
        doReturn(
                getKeywordBidResponseDto(2,1,null)
        ).when(keywordBidService).deleteByCampaignIdAndKeyword(any(KeywordBidRequestDtos.class));
        final String url = "/api/bid/delete";
        final KeywordBidRequestDtos body =
                getRequest(1L, List.of(getKeywordBidDto(100L,"k1"),getKeywordBidDto(200L,"k2")));
        final ResultActions result = mockMvc.perform(delete(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        result.andExpectAll(
                status().isOk(),
                jsonPath("data.requestNumber").value("2"),
                jsonPath("data.responseNumber").value("1")
        ).andDo(print()); // 빈 값이 Json으로 변환되지 않는 것을 확인하기 위해 추가.
    }

    public KeywordBidResponseDto getKeywordBidResponseDto(int requestNum,
                                                          int responseNum,
                                                          List<KeywordBidDto> data){
        return KeywordBidResponseDto.builder()
                .requestNumber(requestNum)
                .responseNumber(responseNum)
                .response(data)
                .build();
    }
    public KeywordBidRequestDtos getRequest(Long campaignId, List<KeywordBidDto> list){
        return KeywordBidRequestDtos.builder()
                .campaignId(campaignId)
                .data(list)
                .build();
    }
    public KeywordBidDto getKeywordBidDto(Long bid, String key){
        return KeywordBidDto.builder()
                .bid(bid)
                .keyword(key)
                .build();
    }

    public KeywordResponseDto getKeywordResDto(){
        return KeywordResponseDto.builder()
                .keyKeyword("keyword")
                .adCost(1.0)
                .clickRate(0.8)
                .clicks(10L)
                .adSales(1.0)
                .cpc(1.0)
                .cvr(1.0)
                .impressions(1L)
                .roas(1.0)
                .keySearchType("test")
                .totalSales(1L)
                .bid(100L)
                .build();
    }
}
