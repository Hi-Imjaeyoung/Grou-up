package growup.spring.springserver.campaign;

import com.nimbusds.jose.shaded.gson.*;
import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.campaign.controller.CampaignController;
import growup.spring.springserver.campaign.dto.CampaignDeleteDto;
import growup.spring.springserver.campaign.dto.CampaignResponseDto;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.campaignoptiondetails.service.CampaignOptionDetailsService;
import growup.spring.springserver.execution.dto.ExecutionMarginResDto;
import growup.spring.springserver.execution.dto.ExecutionResponseDto;
import growup.spring.springserver.execution.service.ExecutionService;
import growup.spring.springserver.global.config.JwtTokenProvider;
import growup.spring.springserver.keyword.service.KeywordService;
import growup.spring.springserver.margin.service.MarginService;
import growup.spring.springserver.memo.MemoControllerTest;
import growup.spring.springserver.memo.service.MemoService;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(CampaignController.class)
public class CampaignControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CampaignService campaignService;
    @MockBean
    private KeywordService keywordService;
    @MockBean
    private MarginService marginService;
    @MockBean
    private MemoService memoService;
    @MockBean
    private ExecutionService executionService;
    @MockBean
    private CampaignOptionDetailsService campaignOptionDetailsService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("getMyCampaigns : ErrorCase1.인가되지 않은 사용자 접근")
    void test1() throws Exception {
        final String url = "/api/campaign/getMyCampaigns";
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(url));
        resultActions.andDo(print()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("getMyCampaigns : Success1.접근 성공")
    @WithAuthUser
    void test2() throws Exception {
        Gson gson = new Gson();
        final String url = "/api/campaign/getMyCampaigns";
        doReturn(List.of(
                getCampaignResDto("name1",1L),
                getCampaignResDto("name2",2L),
                getCampaignResDto("name3",3L)
        )).when(campaignService).getMyCampaigns("test@test.com");
        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(new ArrayList<String>())));

        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("message").value("success : load campaign name list"),
                jsonPath("data").isArray(), // data가 배열인지 확인
                jsonPath("data[0].title").value("name1"), // 첫 번째 요소 검증
                jsonPath("data[1].title").value("name2"), // 두 번째 요소 검증
                jsonPath("data[2].title").value("name3") , // 세 번째 요소 검증
                jsonPath("data[0].campaignId").value(1L), // 첫 번째 요소 검증
                jsonPath("data[1].campaignId").value(2L), // 두 번째 요소 검증
                jsonPath("data[2].campaignId").value(3L)  // 세 번째 요소 검증
        ).andDo(print());
    }

    @Test
    @WithAuthUser
    @DisplayName("캠패인 삭제 api - 실패 id 누락 ")
    void deleteCampaign() throws Exception {
        Gson gson = new Gson();
        final String url = "/api/campaign/deleteCampaign";
        final List<Long> campaignIds = new ArrayList<>();
        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(campaignIds))
                        .with(csrf()));
        resultActions.andExpectAll(
                status().isBadRequest(),
                jsonPath("errorMessage").value("잘못된 요청값 입니다.")
        ).andDo(print());
    }

    @Test
    @WithAuthUser
    @DisplayName("캠패인 삭제 api - 성공")
    void deleteCampaign2() throws Exception {
        Gson gson = new Gson();
        doReturn(2).when(campaignService).deleteCampaign(any());
        final String url = "/api/campaign/deleteCampaign";
        final List<Long> campaignIds = List.of(1L,2L);
        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(campaignIds))
                        .with(csrf()));
        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("data").value("2")
        ).andDo(print());
    }

    @ParameterizedTest
    @WithAuthUser
    @DisplayName("캠패인 데이터 기간 삭제 API : 실패 body 값 누락")
    @MethodSource("provideInvalidCampaignDeleteRequests")
    void deleteCampaignData1(CampaignDeleteDto body) throws Exception {
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();
        String url = "/api/campaign/deleteCampaignData";

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(body))
                        .with(csrf())
        );

        resultActions.andExpect(status().isBadRequest())
                .andDo(print());
    }

    public static Stream<Arguments> provideInvalidCampaignDeleteRequests() {
        return Stream.of(
                // LocalDate 형식 오류 (종료 날짜만 제공)
                Arguments.of(CampaignDeleteDto.builder()
                        .end(LocalDate.now())
                        .campaignIds(List.of(1L))
                        .build()),
                // 시작 날짜가 끝 날짜보다 늦거나 캠페인 ID 누락
                Arguments.of(CampaignDeleteDto.builder()
                        .start(LocalDate.now())
                        .end(LocalDate.now()) // 시작 날짜가 종료 날짜보다 늦음
                        .campaignIds(List.of())
                        .build())
        );
    }
    @Test
    @WithAuthUser
    @DisplayName("캠패인 데이터 기간 삭제 API : 실패 조회 기간 오류")
    void deleteCampaignData2() throws Exception {
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();
        String url = "/api/campaign/deleteCampaignData";
        CampaignDeleteDto body = CampaignDeleteDto.builder()
                .start(LocalDate.now())
                .end(LocalDate.now().minusDays(1))
                .campaignIds(List.of(1L))
                .build();
        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(body))
                        .with(csrf())
        );

        resultActions.andExpectAll(
                status().isBadRequest(),
                jsonPath("errorMessage").value("날짜 형식이 이상합니다.")
        ).andDo(print());
    }

    @Test
    @WithAuthUser
    @DisplayName("캠패인 데이터 기간 삭제 API : 성공")
    void deleteCampaignData3() throws Exception {
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();
        String url = "/api/campaign/deleteCampaignData";
        doReturn(1).when(keywordService).deleteKeywordByCampaignIdsAndDate(any(),any(),any());
        doReturn(1).when(memoService).deleteKeywordByCampaignIdsAndDate(any(),any(),any());
        doReturn(1).when(marginService).deleteKeywordByCampaignIdsAndDate(any(),any(),any());
        doReturn(List.of(ExecutionMarginResDto.builder().build())).when(executionService).getMyExecutionData(any());
        doReturn(1).when(campaignOptionDetailsService).deleteKeywordByExecutionIdsAndDate(any(),any(),any());

        CampaignDeleteDto body = CampaignDeleteDto.builder()
                .start(LocalDate.now().minusDays(1))
                .end(LocalDate.now())
                .campaignIds(List.of(1L))
                .build();
        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(body))
                        .with(csrf())
        );

        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("data.campaignOptionDetail").value(1),
                jsonPath("data.margin").value(1),
                jsonPath("data.memo").value(1),
                jsonPath("data.keyword").value(1)
        ).andDo(print());
    }

    public CampaignResponseDto getCampaignResDto(String name,Long id){
        return CampaignResponseDto.builder()
                .title(name)
                .campaignId(id)
                .build();

    }
    class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public JsonElement serialize(LocalDate date, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(date.format(formatter));
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return LocalDate.parse(json.getAsString(), formatter);
        }
    }
}
