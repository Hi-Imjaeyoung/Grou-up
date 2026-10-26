package growup.spring.springserver.margin.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.global.config.GsonConfig;
import growup.spring.springserver.global.config.JwtTokenProvider;
import growup.spring.springserver.margin.dto.*;
import growup.spring.springserver.margin.service.MarginService;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(MarginController.class)
class MarginControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private MarginService marginService;
    @MockBean
    private CampaignService campaignService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("getCampaignAllSales() : error 1. 데이터 누락")
    @WithAuthUser
    void test1() throws Exception {
        final String badUrl = "/api/margin/getCampaignAllSales?date=";
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders
                .get(badUrl));
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getCampaignAllSales() : 성공 케이스")
    @WithAuthUser
    void testGetCampaignAllSalesSuccess() throws Exception {
        Gson gson = new Gson();
        // Given
        final String url = "/api/margin/getCampaignAllSales?date=2024-12-01";
        LocalDate targetDate = LocalDate.of(2024, 11, 11);
        List<MarginSummaryResponseDto> mockResponse = List.of(
                MarginSummaryResponseDto.builder()
                        .date(targetDate)
                        .campaignId(1L)
                        .campaignName("Campaign 1")
                        .todaySales(300.0)
                        .yesterdaySales(200.0)
                        .differentSales(100.0)
                        .build(),
                MarginSummaryResponseDto.builder()
                        .date(targetDate)
                        .campaignId(2L)
                        .campaignName("Campaign 2")
                        .todaySales(250.0)
                        .yesterdaySales(150.0)
                        .differentSales(100.0)
                        .build()
        );
        doReturn(mockResponse).when(marginService).getCampaignAllSales(anyString(), any(LocalDate.class));
        System.out.println("mockResponse = " + mockResponse);
        // When
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders
                .get(url));

        // then
        resultActions.andDo(print());

        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("$.message").value("success : getMyCampaignDetails"),
                jsonPath("$.data").isArray(),
                jsonPath("$.data[0].date").value("2024-11-11"),
                jsonPath("$.data[0].campaignId").value(1),
                jsonPath("$.data[0].campaignName").value("Campaign 1"),
                jsonPath("$.data[0].yesterdaySales").value(200.0),
                jsonPath("$.data[0].todaySales").value(300.0),
                jsonPath("$.data[0].differentSales").value(100.0),
                jsonPath("$.data[1].campaignId").value(2),
                jsonPath("$.data[1].campaignName").value("Campaign 2"),
                jsonPath("$.data[1].todaySales").value(250.0)
        );
    }

    @Test
    @WithAuthUser
    @DisplayName("getMarginOverviewGraph() : 성공 케이스")
    void getMarginOverviewGraph() throws Exception {
        // Given
        LocalDate start = LocalDate.of(2024, 12, 1);
        LocalDate end = LocalDate.of(2024, 12, 15);
        List<DailyAdSummaryDto> mockResponse = List.of(
                new DailyAdSummaryDto(start, 200.0, 400.0),
                new DailyAdSummaryDto(end, 300.0, 600.0)
        );

        // Mocking 서비스 호출
        doReturn(mockResponse).when(marginService).getMarginOverviewGraph(any(LocalDate.class), any(LocalDate.class),any(String.class));

        // API 호출 URL
        final String url = "/api/margin/getMarginOverviewGraph?start=2024-12-01&end=2024-12-15";

        // When & Then
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders
                .get(url));
        resultActions.andDo(print());
        resultActions
                .andExpect(status().isOk()) // HTTP 상태 코드 200
                .andExpect(jsonPath("$.message").value("success : getMarginOverviewGraph")) // 응답 메시지 검증
                .andExpect(jsonPath("$.data").isArray()) // 데이터가 배열인지 확인
                .andExpect(jsonPath("$.data[0].marDate").value("2024-12-01"))
                .andExpect(jsonPath("$.data[0].marSales").value(200.0))
                .andExpect(jsonPath("$.data[1].marDate").value("2024-12-15"))
                .andExpect(jsonPath("$.data[1].marSales").value(300.0));
        // Verify 서비스 호출
        verify(marginService).getMarginOverviewGraph(any(LocalDate.class), any(LocalDate.class),any(String.class));
    }

    @Test
    @DisplayName("getNetProfitAndReturnCost() : success 1. 빈값 리턴")
    @WithAuthUser
    void getNetProfitAndReturnCost_success1() throws Exception {

        Gson gson = new Gson();
        // Given
        final String url = "/api/margin/getNetProfitAndReturnCost?startDate=2024-12-01&endDate=2024-12-05";

        doReturn(new ArrayList<>()).when(marginService).getDailyTotalMarginListResDto(any(LocalDate.class), any(LocalDate.class), any(String.class));

        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders
                .get(url));

        resultActions.andDo(print());
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success : getNetProfitAndReturnCost"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("getNetProfitAndReturnCost() : success 1. 알맞는 값 리턴")
    @WithAuthUser
    void getNetProfitAndReturnCost_success2() throws Exception {
        Gson gson = new Gson();
        List<DailyNetProfitResponseDto> ResponseDto = List.of(
                new DailyNetProfitResponseDto(LocalDate.of(2024, 12, 12), 10.0, 10.0, 10L,10.0),
                new DailyNetProfitResponseDto(LocalDate.of(2024, 12, 13), 9.0, 9.0, 10L,20.0),
                new DailyNetProfitResponseDto(LocalDate.of(2024, 12, 14), 8.0, 8., 100L,30.0)
        );
        doReturn(ResponseDto).when(marginService).getDailyTotalMarginListResDto(any(LocalDate.class), any(LocalDate.class), any(String.class));

        final String url = "/api/margin/getNetProfitAndReturnCost?startDate=2024-12-01&endDate=2024-12-05";

        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders
                .get(url));

        resultActions.andDo(print());
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success : getNetProfitAndReturnCost"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].marDate").value("2024-12-12"))
                .andExpect(jsonPath("$.data[1].marDate").value("2024-12-13"))
                .andExpect(jsonPath("$.data[2].marDate").value("2024-12-14"))
                .andExpect(jsonPath("$.data[0].margin").value(10.0))
                .andExpect(jsonPath("$.data[1].margin").value(9.0));

        verify(marginService).getDailyTotalMarginListResDto(any(LocalDate.class), any(LocalDate.class), any(String.class));
    }

    @Test
    @DisplayName("getDailyMarginSummary() : success 1. 알맞는 값 리턴")
    @WithAuthUser
    void getDailyMarginSummary_success1() throws Exception {
        Gson gson = new Gson();

        List<DailyMarginSummary> ResponsDto = List.of(
                new DailyMarginSummary("방한마스크1", 100L, 100.0),
                new DailyMarginSummary("방한마스크2", 1100L, 100.0)
        );

        doReturn(ResponsDto).when(marginService).getDailyMarginSummary(any(List.class), any(LocalDate.class), any(LocalDate.class));

        final String url = "/api/margin/getDailyMarginSummary?start=2025-01-01&end=2025-01-31";

        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders
                .get(url));

        resultActions.andDo(print());
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success : getMyCampaignDetails"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].marAdMargin").value(100L))
                .andExpect(jsonPath("$.data[1].marAdMargin").value(1100L));

    }

    @Test
    @DisplayName("updateEfficiencyAndAdBudget() : failCase1. campaignId 없음")
    @WithAuthUser
    void updateEfficiencyAndAdBudget_fail1() throws Exception {
        Gson gson = GsonConfig.getGson();
        final String url = "/api/margin/updateEfficiencyAndAdBudget";
        final MarginUpdateRequestDtos body = MarginUpdateRequestDtos.builder()
                .campaignId(null)
                .data(List.of(
                        MarginUpdateRequestDto.builder()
                                .id(1L)
                                .marDate(LocalDate.of(2025, 1, 1))
                                .marAdBudget(10.0)
                                .marTargetEfficiency(10.0)
                                .build()
                ))
                .build();

        ResultActions result = mockMvc.perform(post(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        result.andExpectAll(
                status().isBadRequest(),
                jsonPath("$.errorMessage").value("잘못된 요청 형식입니다.")
        );
    }

    @Test
    @DisplayName("updateEfficiencyAndAdBudget() : failCase2. data 빔")
    @WithAuthUser
    void updateEfficiencyAndAdBudget_fail2() throws Exception {
        Gson gson = GsonConfig.getGson();

        final String url = "/api/margin/updateEfficiencyAndAdBudget";
        MarginUpdateRequestDtos invalidBody = MarginUpdateRequestDtos.builder()
                .campaignId(1L)
                .data(Collections.emptyList())
                .build();

        ResultActions result = mockMvc.perform(post(url)
                .content(gson.toJson(invalidBody))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        result.andExpectAll(
                status().isBadRequest(),
                jsonPath("$.errorMessage").value("잘못된 요청 형식입니다.")
        );
    }

    @Test
    @DisplayName("updateEfficiencyAndAdBudget() : successCase. ")
    @WithAuthUser
    void updateEfficiencyAndAdBudget_success() throws Exception {
        Gson gson = GsonConfig.getGson();

        final String url = "/api/margin/updateEfficiencyAndAdBudget";
        final MarginUpdateRequestDtos body = MarginUpdateRequestDtos.builder()
                .campaignId(1L)
                .data(List.of(
                        MarginUpdateRequestDto.builder()
                                .id(1L)
                                .marDate(LocalDate.of(2025, 1, 1))
                                .marAdBudget(10.0)
                                .marTargetEfficiency(10.0)
                                .build(),
                        MarginUpdateRequestDto.builder()
                                .id(2L)
                                .marDate(LocalDate.of(2025, 1, 2))
                                .marAdBudget(15.0)
                                .marTargetEfficiency(15.0)
                                .build()
                ))
                .build();


        ResultActions result = mockMvc.perform(post(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        result.andExpectAll(
                status().isOk(),
                jsonPath("$.message").value("success : updateEfficiencyAndAdBudget")
        );
    }

    @Test
    @DisplayName("createMarginTable() : successCase. 새로운 Margin 생성 성공")
    @WithAuthUser
    void createMarginTable_successCase() throws Exception {
        Gson gson = GsonConfig.getGson();
        // Given
        final String url = "/api/margin/createMarginTable?targetDate=2025-11-12&campaignId=1";
        Long mockMarginId = 100L;

        doReturn(mockMarginId).when(marginService).createMarginTable(any(LocalDate.class), any(Long.class), any(String.class));

        // When
        ResultActions result = mockMvc.perform(post(url)
                .content(gson.toJson(mockMarginId))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));


        // Then
        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success : createMarginTable"))
                .andExpect(jsonPath("$.data").value(mockMarginId));

        verify(marginService).createMarginTable(any(LocalDate.class), any(Long.class), any(String.class));
    }
    @Test
    @DisplayName("findLatestMarginDateByEmail() : successCase")
    @WithAuthUser
    void findLatestMarginDateByEmail_successCase() throws Exception {
        // Given
        Gson gson = GsonConfig.getGson();
        final String url = "/api/margin/findLatestMarginDateByEmail";
        LocalDate mockDate = LocalDate.of(2024, 12, 1);

        doReturn(mockDate).when(marginService).findLatestMarginDateByEmail(any(String.class));

        // When
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(url));

        // Then
        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success : findLatestMarginDateByEmail"))
                .andExpect(jsonPath("$.data").value(mockDate.toString()));

        verify(marginService).findLatestMarginDateByEmail(any(String.class));
    }

    @ParameterizedTest
    @DisplayName("getMarginOverview() : 날짜 검증 실패 케이스")
    @WithAuthUser
    @MethodSource("invalidDateRange")
    void getMarginOverview_invalidDateRange(String url) throws Exception {
        // When
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(url)
                .with(csrf()));

        // Then
        result.andDo(print())
                .andExpectAll(
                status().isOk(),
                jsonPath("$.message").value("요청 날짜 형식 오류로 인한 빈 값 리턴."));
    }
    static Stream<String> invalidDateRange() {
        return Stream.of(
                "/api/margin/getMarginOverview?start=2025-06-15&end=2025-06-09",
                "/api/margin/getMarginOverview?start=2025-06-15"
        );
    }

    @DisplayName("getMarginOverview() : 성공 케이스")
    @WithAuthUser
    @Test
    void getMarginOverview_successCase() throws Exception {
        final String url = "/api/margin/getMarginOverview?start=2025-06-15&end=2025-06-21";

        List<MarginOverviewResponseDto> mockResponse = List.of(
                new MarginOverviewResponseDto(1L, "Campaign 1", 1500.0, 200.0, 20.0, 50.0, 300.0, 10L, 50.0, 5L, 10.0),
                new MarginOverviewResponseDto(2L, "Campaign 2", 1000.0, 300.0, 20.0, 60.0, 400.0, 15L, 60.0, 8L, 12.5)
        );
        doReturn(mockResponse).when(marginService).getMarginOverview(any(LocalDate.class), any(LocalDate.class), any(String.class));

        ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(url)
                .with(csrf()));

        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success : getMarginOverview"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].campaignId").value(1L))
                .andExpect(jsonPath("$.data[1].campaignId").value(2L));


    }
}