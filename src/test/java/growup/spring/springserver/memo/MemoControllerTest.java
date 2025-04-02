package growup.spring.springserver.memo;

import com.nimbusds.jose.shaded.gson.*;
import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.memo.MemoNotFoundException;
import growup.spring.springserver.global.config.JwtTokenProvider;
import growup.spring.springserver.memo.controller.MemoController;
import growup.spring.springserver.memo.domain.Memo;
import growup.spring.springserver.memo.dto.MemoRequestDto;
import growup.spring.springserver.memo.dto.MemoUpdateRequestDto;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(MemoController.class)
public class MemoControllerTest {
    @Autowired
    private MockMvc mockMvc;
    private Gson gson;
    @MockBean
    private MemoService memoService;
    @MockBean
    private CampaignService campaignService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @DisplayName("메모 post 실패 : body 필수 값 누락")
    @Test
    @WithAuthUser
    void postMemo_fail() throws Exception {
        gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();
        String url1 = "/api/memo/post";
        MemoRequestDto body = MemoRequestDto.builder()
                .date(LocalDate.parse("2013-01-12"))
                .contents("test")
                .build();
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(url1)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())); // 403 에러 해결을 위해 추가
        resultActions.andExpectAll(
                status().isBadRequest(),
                jsonPath("errorMessage").value("메모할 캠패인 id를 보내야합니다.")
        ).andDo(print());
    }
    @DisplayName("메모 post 성공")
    @Test
    @WithAuthUser
    void postMemo_success() throws Exception {
        Campaign campaign = getCampaign();
        doReturn(campaign).when(campaignService).getMyCampaign(any(Long.class),any(String.class));
        doReturn(getMemo(1L,"contents",campaign,LocalDate.now())).when(memoService).makeMemo(any(Campaign.class),any(MemoRequestDto.class));
        gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();
        String url1 = "/api/memo/post";
        MemoRequestDto body = MemoRequestDto.builder()
                .campaignId(1L)
                .date(LocalDate.parse("2013-01-12"))
                .contents("test")
                .build();
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(url1)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())); // 403 에러 해결을 위해 추가
        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("data.id").value("1"),
                jsonPath("data.date").value("03-12")
        ).andDo(print());
    }

    @DisplayName("캠패인에 대한 메모 조회 성공")
    @Test
    @WithAuthUser
    void getMemoAboutCampaign() throws Exception {
        Campaign campaign = getCampaign();
        doReturn(campaign).when(campaignService).getMyCampaign(1234L, "test@test.com");
        doReturn(List.of(getMemo(1L,"memo1",campaign,LocalDate.now()),
                getMemo(2L,"memo2",campaign,LocalDate.now()),
                getMemo(3L,"memo3",campaign,LocalDate.now()))).when(memoService).getMemoAboutCampaign(campaign);
        String url = "/api/memo/getMemoAboutCampaign?campaignId=1234";
        gson = new Gson();
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(url));
        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("data[0].contents").value("memo1"),
                jsonPath("data[1].contents").value("memo2"),
                jsonPath("data[2].contents").value("memo3")
        ).andDo(print());
    }

    @DisplayName("메모 수정 성공")
    @Test
    @WithAuthUser
    void updateMemo() throws Exception{
        MemoUpdateRequestDto memoRequestDto = MemoUpdateRequestDto.builder()
                .memoId(1L)
                .contents("update Memo")
                .build();
        doReturn(getMemo(1L,"update Memo",getCampaign(),LocalDate.now())).when(memoService).updateMemo(any(MemoUpdateRequestDto.class));
        String url = "/api/memo/update";
        gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.patch(url)
                .content(gson.toJson(memoRequestDto))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("data.contents").value("update Memo")
        ).andDo(print());
    }

    @DisplayName("메모 수정 실패: 필수 요청값 누락")
    @Test
    @WithAuthUser
    void updateMemo_fail() throws Exception{
        MemoUpdateRequestDto memoRequestDto = MemoUpdateRequestDto.builder()
                .memoId(1L)
                .build();
        doReturn(getMemo(1L,"update Memo",getCampaign(),LocalDate.now())).when(memoService).updateMemo(any(MemoUpdateRequestDto.class));
        String url = "/api/memo/update";
        gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.patch(url)
                .content(gson.toJson(memoRequestDto))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        resultActions.andExpectAll(
                status().isBadRequest(),
                jsonPath("errorMessage").value("수정할 내용을 보내야합니다.")
        ).andDo(print());
    }

    @DisplayName("메모 수정 실패: 수정할 메모 id를 찾지 못함")
    @Test
    @WithAuthUser
    void updateMemo_fail2() throws Exception{
        MemoUpdateRequestDto memoRequestDto = MemoUpdateRequestDto.builder()
                .memoId(1L)
                .contents("contents")
                .build();
        doThrow(new MemoNotFoundException()).when(memoService).updateMemo(any(MemoUpdateRequestDto.class));
        String url = "/api/memo/update";
        gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.patch(url)
                .content(gson.toJson(memoRequestDto))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        resultActions.andExpectAll(
                status().isBadRequest(),
                jsonPath("errorMessage").value("해당 메모가 존재하지 않습니다")
        ).andDo(print());
    }

    @DisplayName("메모 삭제 성공")
    @Test
    @WithAuthUser
    void deleteMemo() throws Exception {
        String url = "/api/memo/delete?memoId=1";
        doReturn(1).when(memoService).deleteMemo(any(Long.class));
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.delete(url)
                .with(csrf()));
        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("data").value("삭제 성공")
        );
    }

    @DisplayName("메모 삭제 실패 : DB에 해당 Id 없음")
    @Test
    @WithAuthUser
    void deleteMemo_fail() throws Exception {
        String url = "/api/memo/delete?memoId=1";
        doReturn(0).when(memoService).deleteMemo(any(Long.class));
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.delete(url)
                .with(csrf()));
        resultActions.andExpectAll(
                status().isBadRequest(),
                jsonPath("errorMessage").value("해당 메모가 존재하지 않습니다")
        );
    }

    @DisplayName("특정 기간 내 메모 조회 실패 : 요청 값 누락")
    @ParameterizedTest
    @WithAuthUser
    @MethodSource("GetMemoByDateIncorrectParam")
    void findMemoByDateAndCampaignId(String start,String end, Long campaignId) throws Exception {
        String url = "/api/memo/getMemoByDate";
        ResultActions resultActions;
        if(campaignId==null){
            resultActions = mockMvc.perform(MockMvcRequestBuilders.get(url)
                    .param("start",start)
                    .param("end",end));
        }else{
            resultActions = mockMvc.perform(MockMvcRequestBuilders.get(url)
                    .param("start",start)
                    .param("end",end)
                    .param("campaignId", String.valueOf(campaignId)));
        }
        resultActions.andExpectAll(
                status().isBadRequest(),
                jsonPath("errorMessage").value("잘못된 요청값 입니다.")
        ).andDo(print());
    }
    public static Stream<Arguments> GetMemoByDateIncorrectParam(){
        return Stream.of(
                Arguments.of("","2012-01-01",1L),
                Arguments.of("2012-01-01","",1L),
                Arguments.of("2012-01-01","2012-01-05",null));
    }
    @DisplayName("특정 기간 내 메모 조회 성공")
    @Test
    @WithAuthUser
    void findMemoByDateAndCampaignId2() throws Exception {
        String url = "/api/memo/getMemoByDate";
        Map<String,List<String>> map = new HashMap<>();
        map.put("2025-04-01",List.of("memo1"));
        map.put("2025-04-02",List.of("memo3"));
        doReturn(map).when(memoService).getMemoByDateAndCampaign(any(),any(),any());
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(url)
                    .param("start","2025-04-01")
                    .param("end","2025-04-01")
                    .param("campaignId", String.valueOf(1L)));
        resultActions.andExpectAll(
                status().isOk()
        ).andDo(print());
    }

    private Campaign getCampaign(){
        return Campaign.builder().build();
    }
    private Memo getMemo(Long id, String content, Campaign campaign, LocalDate localDate){
        return Memo.builder()
                .campaign(campaign)
                .id(id)
                .contents(content)
                .date(localDate)
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
