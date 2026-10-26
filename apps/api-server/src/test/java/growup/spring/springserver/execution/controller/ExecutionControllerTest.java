package growup.spring.springserver.execution.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.execution.domain.Execution;
import growup.spring.springserver.execution.dto.ExecutionRequestDto;
import growup.spring.springserver.execution.dto.ExecutionResponseDto;
import growup.spring.springserver.execution.service.ExecutionService;
import growup.spring.springserver.global.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.execution.dto.ExecutionDto;
import growup.spring.springserver.execution.dto.ExecutionMarginResDto;
import growup.spring.springserver.execution.dto.ExecutionRequestDtos;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(ExecutionController.class)
class ExecutionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    private Gson gson;
    @MockBean
    private ExecutionService executionService;
    @MockBean
    private CampaignService campaignService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

//    @DisplayName("/api/execution/getByCampaignIdAndExeIds : Param 값 누락 시")
//    @ParameterizedTest
//    @MethodSource("nullBodies")
//    @WithAuthUser
//    void test1_1(Long campaignId, List<Long> lists) throws Exception {
//        gson = new Gson();
//        final String url = "/api/execution/getByCampaignIdAndExeIds";
//        final ExecutionRequestDto executionRequestDto = ExecutionRequestDto.builder()
//                .exeIds(lists)
//                .campaignId(campaignId)
//                .build();
//        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
//                .get(url)
//                .param("campaignId", String.valueOf(campaignId));
//
//        for (Long exeId : lists) {
//            requestBuilder.param("exeIds", String.valueOf(exeId));
//        }
//
//        final ResultActions resultActions = mockMvc.perform(requestBuilder);
//        resultActions.andExpectAll(
//                status().isBadRequest(),
//                jsonPath("errorMessage").value("잘못된 요청값 입니다.")
//        ).andDo(print());
//    }
//
//    private static Stream<Arguments> nullBodies(){
//        return Stream.of(
//                Arguments.of(1L,List.of())
//        );
//    }

//    @DisplayName("/api/execution/getByCampaignIdAndExeIds : Success")
//    @Test
//    @WithAuthUser
//    void test1_2() throws Exception {
//        gson = new Gson();
//        final String url = "/api/execution/getByCampaignIdAndExeIds";
//        List<Long> lists = List.of(1L, 2L, 3L);
//        Long campaignId = 1L;
//
//        doReturn(List.of(getExecution(1L, "option1"),
//                getExecution(2L, "option2"),
//                getExecution(3L, "option3"))).when(executionService)
//                .getExecutionsByCampaignIdAndExeIds(any(Long.class), any(List.class));
//
//        // exeIds 파라미터를 여러 개로 전달
//        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
//                .get(url)
//                .param("campaignId", String.valueOf(campaignId));
//
//        for (Long exeId : lists) {
//            requestBuilder.param("exeIds", String.valueOf(exeId));
//        }
//
//        final ResultActions resultActions = mockMvc.perform(requestBuilder);
//
//        resultActions.andExpectAll(
//                status().isOk(),
//                jsonPath("message").value("get Api success"),
//                jsonPath("data[0].exeId").value(1L),
//                jsonPath("data[0].exeProductName").value("option1")
//        ).andDo(print());
//    }

    public Execution getExecution(Long exeId, String name){
        return Execution.builder()
                .exeId(exeId)
                .exeProductName(name)
                .build();
    }

    @WithAuthUser
    @Test
    @DisplayName("getMyExecutionData() : ErrorCase 1. 캠페인 ID 누락")
    void getMyExecutionData_Fail() throws Exception {

        final String url = "/api/execution/getMyExecutionData?";

        ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(url));
        result.andExpect(status().isBadRequest());
    }

    @WithAuthUser
    @Test
    @DisplayName("getMyExecutionData() : SuccessCase 1. 정상 호출")
    void getMyExecutionData_Success() throws Exception {
        gson = new Gson();

        final String url = "/api/execution/getMyExecutionData?campaignId=1";
        // Mock data
        List<ExecutionMarginResDto> mockData = List.of(
                ExecutionMarginResDto.builder()
                        .exeId(1L)
                        .exeDetailCategory("빨강색")
                        .exeSalePrice(10000L)
                        .exeTotalPrice(15000L)
                        .exeCostPrice(5000L)
                        .build(),
                ExecutionMarginResDto.builder()
                        .exeId(2L)
                        .exeDetailCategory("파랑색")
                        .exeSalePrice(20000L)
                        .exeTotalPrice(25000L)
                        .exeCostPrice(10000L)
                        .build()
        );

        // Mock behavior
        when(executionService.getMyExecutionData(any(Long.class))).thenReturn(mockData);

        ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(url)
                .content(gson.toJson(mockData))
                .accept(MediaType.APPLICATION_JSON));

        // Expect 200 OK
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].exeId").value(1L))
                .andExpect(jsonPath("$.data[0].exeDetailCategory").value("빨강색"))
                .andExpect(jsonPath("$.data[0].exeSalePrice").value(10000L))
                .andExpect(jsonPath("$.data[0].exeTotalPrice").value(15000L))
                .andExpect(jsonPath("$.data[0].exeCostPrice").value(5000L))
                .andExpect(jsonPath("$.data[1].exeId").value(2L))
                .andExpect(jsonPath("$.data[1].exeDetailCategory").value("파랑색"))
                .andExpect(jsonPath("$.data[1].exeSalePrice").value(20000L))
                .andExpect(jsonPath("$.data[1].exeTotalPrice").value(25000L))
                .andExpect(jsonPath("$.data[1].exeCostPrice").value(10000L));
    }

    private static Stream<Arguments> incorrectBodies() {
        return Stream.of(
                Arguments.of(ExecutionRequestDtos.builder()
                        .campaignId(1L)
                        .data(List.of())
                        .build()
                ),
                Arguments.of(ExecutionRequestDtos.builder()
                        .campaignId(null)
                        .data(List.of(ExecutionDto.builder().build()))
                        .build())
        );
    }

    @WithAuthUser
    @DisplayName("updateMyExecutionData() Fail Case1. body 데이터 누락 ")
    @ParameterizedTest
    @MethodSource("incorrectBodies")
    void updateMyExecutionData_Failcase1(ExecutionRequestDtos body) throws Exception {
        gson = new Gson();
        final String url = "/api/execution/update";
        final ResultActions result = mockMvc.perform(patch(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        result.andExpectAll(
                status().isBadRequest()
        );
    }

    @WithAuthUser
    @DisplayName("updateMyExecutionData() Success Case1. 성공")
    @Test
    void updateMyExecutionData_SuccessCase1() throws Exception {
        Gson gson = new Gson();
        doReturn(getCampaign()).when(campaignService).getMyCampaign(any(Long.class),any(String.class));

        doReturn(
                getExecutionResponseDto(2, 2)
        ).when(executionService)
                .updateExecutions(any(ExecutionRequestDtos.class), any(Campaign.class));

        final String url = "/api/execution/update";
        final ExecutionRequestDtos body =
                getExecutionRequestDtos(1L,
                        List.of(getExecutionDto(1L, 1L, 1L, 1L, 1.1, 1.1),
                                getExecutionDto(2L, 1L, 1L, 1L, 1.1, 1.1)));

        final ResultActions result = mockMvc.perform(patch(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));

        result.andExpectAll(
                status().isOk(),
                jsonPath("data.requestNumber").value("2"),
                jsonPath("data.responseNumber").value("2")
                );
    }

    public ExecutionRequestDtos getExecutionRequestDtos(Long CampaignId, List<ExecutionDto> list) {
        return ExecutionRequestDtos.builder()
                .campaignId(CampaignId)
                .data(list)
                .build();
    }
    public ExecutionDto getExecutionDto(Long executionId,
                                        Long exeSalePrice,
                                        Long exeTotalPrice,
                                        Long exeCostPrice,
                                        Double exePerPiece,
                                        Double exeZeroRoas) {
        return ExecutionDto.builder()
                .executionId(executionId)
                .exeSalePrice(exeSalePrice)
                .exeTotalPrice(exeTotalPrice)
                .exeCostPrice(exeCostPrice)
                .exePerPiece(exePerPiece)
                .exeZeroRoas(exeZeroRoas)
                .build();
    }

    public ExecutionResponseDto getExecutionResponseDto(int reqeustNum, int responseNum) {
        return ExecutionResponseDto.builder()
                .requestNumber(reqeustNum)
                .responseNumber(responseNum)
                .build();
    }
    public static Campaign getCampaign(){
        return Campaign.builder().campaignId(1L).camCampaignName("testCamp").build();
    }
}

