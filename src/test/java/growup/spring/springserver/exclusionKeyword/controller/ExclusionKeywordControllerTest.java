package growup.spring.springserver.exclusionKeyword.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.exclusionKeyword.dto.ExclusionKeywordRequestDto;
import growup.spring.springserver.exclusionKeyword.dto.ExclusionKeywordResponseDto;
import growup.spring.springserver.exclusionKeyword.service.ExclusionKeywordService;
import growup.spring.springserver.global.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(ExclusionKeywordController.class)
public class ExclusionKeywordControllerTest {
    @Autowired
    private MockMvc mockMvc;
    private Gson gson;
    @MockBean
    private ExclusionKeywordService exclusionKeywordService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @DisplayName("addExclusionKeyword() : Error 1. 캠패인 ID 누락 시")
    @Test
    @WithAuthUser
    void test1_1() throws Exception {
        gson = new Gson();
        final String url = "/api/exclusionKeyword/addExclusionKeyword";
        ExclusionKeywordRequestDto body = ExclusionKeywordRequestDto
                .builder()
                .exclusionKeyword(List.of("key"))
                .campaignId(null)
                .build();
        ResultActions resultActions = mockMvc.perform(post(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())); // 403 에러 해결을 위해 추가
        resultActions.andExpectAll(
                status().isBadRequest(),
                jsonPath("errorMessage").value("잘못된 요청 형식입니다.")
        ).andDo(print());
    }

    @DisplayName("addExclusionKeyword() : Error 2. 제외 키워드 누락 시")
    @Test
    @WithAuthUser
    void test1_2() throws Exception {
        //when
        gson = new Gson();
        final String url = "/api/exclusionKeyword/addExclusionKeyword";
        ExclusionKeywordRequestDto body = ExclusionKeywordRequestDto
                .builder()
                .exclusionKeyword(List.of())
                .campaignId(1L)
                .build();
        //given
        ResultActions resultActions = mockMvc.perform(post(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())); // 403 에러 해결을 위해 추가
        //then
        resultActions.andExpectAll(
                status().isBadRequest(),
                jsonPath("errorMessage").value("잘못된 요청 형식입니다.")
        ).andDo(print());
    }

    @DisplayName("addExclusionKeyword() : Success 1")
    @Test
    @WithAuthUser
    void test1_3() throws Exception {
        //when
        gson = new Gson();
        final String url = "/api/exclusionKeyword/addExclusionKeyword";
        ExclusionKeywordRequestDto body = ExclusionKeywordRequestDto
                .builder()
                .exclusionKeyword(List.of("key","key2"))
                .campaignId(1L)
                .build();
        //given
        doReturn(ExclusionKeywordResponseDto.builder()
                .requestData(5)
                .responseData(4)
                .build()).when(exclusionKeywordService).addExclusionKeyword(any(ExclusionKeywordRequestDto.class),any(String.class));
        ResultActions resultActions = mockMvc.perform(post(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())); // 403 에러 해결을 위해 추가
        //then
        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("message").value("success : add exclusionKeyword"),
                jsonPath("data.requestData").value(5),
                jsonPath("data.responseData").value(4)
        ).andDo(print());
    }

    @DisplayName("removeExclusionKeyword() : Error 1. body 데이터 누락 campaignID")
    @Test
    @WithAuthUser
    void test2_1() throws Exception {
        //when
        gson = new Gson();
        final String url = "/api/exclusionKeyword/remove";
        ExclusionKeywordRequestDto body = ExclusionKeywordRequestDto
                .builder()
                .exclusionKeyword(List.of("key","key2"))
                .campaignId(null)
                .build();
        //then
        ResultActions resultActions = mockMvc.perform(delete(url).content(gson.toJson(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())); // 403 에러 해결을 위해 추가
        resultActions.andExpectAll(
                status().isBadRequest(),
                jsonPath("errorMessage").value("잘못된 요청 형식입니다.")
        ).andDo(print());
    }

    @DisplayName("removeExclusionKeyword() : Error 2. body 데이터 누락 삭제할 키워드가 0개")
    @Test
    @WithAuthUser
    void test2_2() throws Exception {
        //when
        gson = new Gson();
        final String url = "/api/exclusionKeyword/remove";
        ExclusionKeywordRequestDto body = ExclusionKeywordRequestDto
                .builder()
                .exclusionKeyword(List.of())
                .campaignId(1L)
                .build();
        //then
        ResultActions resultActions = mockMvc.perform(delete(url).content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())); // 403 에러 해결을 위해 추가
        resultActions.andExpectAll(
                status().isBadRequest(),
                jsonPath("errorMessage").value("잘못된 요청 형식입니다.")
        ).andDo(print());
    }

    @DisplayName("removeExclusionKeyword() : Success")
    @Test
    @WithAuthUser
    void test2_3() throws Exception {
        //when
        gson = new Gson();
        final String url = "/api/exclusionKeyword/remove";
        ExclusionKeywordRequestDto body = ExclusionKeywordRequestDto
                .builder()
                .exclusionKeyword(List.of("key1","key2"))
                .campaignId(1L)
                .build();
        doReturn(true).when(exclusionKeywordService).deleteExclusionKeyword(any(ExclusionKeywordRequestDto.class),any(String.class));
        //then
        ResultActions resultActions = mockMvc.perform(delete(url).content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())); // 403 에러 해결을 위해 추가
        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("message").value("success : remove")
        ).andDo(print());
    }

    @DisplayName("getExclusionKeywords() : 파라미터 값 누락")
    @ParameterizedTest
    @WithAuthUser
    @MethodSource("incorrectGetExKeysURl")
    void test3_1(String url) throws Exception {
        //when
        //given
        ResultActions resultActions = mockMvc.perform(get(url)
                .with(csrf()));
        //then
        resultActions.andExpectAll(
                status().isBadRequest()
        ).andDo(print());
    }
    static Stream<String> incorrectGetExKeysURl(){
        return Stream.of("/api/exclusionKeyword/getExclusionKeywords",
                "/api/exclusionKeyword/getExclusionKeywords?campaignId=",
                "/api/exclusionKeyword/getExclusionKeywords?campaignId=noLong"
                );
    }

    @DisplayName("getExclusionKeywords() : Success")
    @Test
    @WithAuthUser
    void test3_2() throws Exception {
        //when
        final String url = "/api/exclusionKeyword/getExclusionKeywords?campaignId=1";
        doReturn(List.of("exK1","exK2","exK3")).when(exclusionKeywordService).getExclusionKeywords(any(Long.class));
        //given
        ResultActions resultActions = mockMvc.perform(get(url));
        //then
        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("data[0]").value("exK1"),
                jsonPath("data[1]").value("exK2"),
                jsonPath("data[2]").value("exK3")
        );

    }

}
