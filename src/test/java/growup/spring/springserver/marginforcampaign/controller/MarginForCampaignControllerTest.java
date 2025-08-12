package growup.spring.springserver.marginforcampaign.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.exception.marginforcampaign.MarginForCampaignIdNotFoundException;
import growup.spring.springserver.global.config.JwtTokenProvider;
import growup.spring.springserver.marginforcampaign.dto.MarginForCampaignResDto;
import growup.spring.springserver.marginforcampaign.dto.MfcDto;
import growup.spring.springserver.marginforcampaign.dto.MfcRequestDtos;
import growup.spring.springserver.marginforcampaign.dto.MfcValidationResponseDto;
import growup.spring.springserver.marginforcampaign.service.MarginForCampaignService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(MarginForCampaignController.class)
class MarginForCampaignControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private Gson gson;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private MarginForCampaignService marginForCampaignService;


    @WithAuthUser
    @Test
    @DisplayName("getExecutionAboutCampaign() : ErrorCase 1. 캠페인 ID 누락")
    void getExecutionAboutCampaign_Fail() throws Exception {

        final String url = "/api/marginforcam/getExecutionAboutCampaign?";

        ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(url));
        result.andExpect(status().isBadRequest());
    }
    @WithAuthUser
    @Test
    @DisplayName("getExecutionAboutCampaign() : SuccessCase 1. 정상 호출")
    void getExecutionAboutCampaign_Succuss() throws Exception {
        final String url = "/api/marginforcam/getExecutionAboutCampaign?campaignId=1";
        gson = new Gson();

        List<MarginForCampaignResDto> mockData = List.of(
                createMarginForCampaignResDto("상품1",1L),
                createMarginForCampaignResDto("상품2",3L)
        );

        // when
        when(marginForCampaignService.marginForCampaignByCampaignId(any(Long.class))).thenReturn(mockData);

        // then
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(url)
                .content(gson.toJson(mockData))
                .accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].mfcProductName").value("상품1"))
                .andExpect(jsonPath("$.data[0].mfcTotalPrice").value(1L))
                .andExpect(jsonPath("$.data[1].mfcProductName").value("상품2"))
                .andExpect(jsonPath("$.data[1].mfcTotalPrice").value(3L));

    }

    private static Stream<Arguments> incorrectBodies() {
        return Stream.of(
                Arguments.of(MfcRequestDtos.builder()
                        .campaignId(1L)
                        .data(List.of())
                        .build()
                ),
                Arguments.of(MfcRequestDtos.builder()
                        .campaignId(null)
                        .data(List.of(MfcDto.builder().build()))
                        .build())
        );
    }


    @WithAuthUser
    @DisplayName("updateMyExecutionData() Fail Case1. body 데이터 누락 ")
    @ParameterizedTest
    @MethodSource("incorrectBodies")
    void updateExecutionAboutCampaign_failCase1(MfcRequestDtos body) throws Exception {
        gson = new Gson();
        final String url = "/api/marginforcam/updateExecutionAboutCampaign";
        final ResultActions result = mockMvc.perform(patch(url)
                .content(gson.toJson(body))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));
        result.andExpectAll(
                status().isBadRequest()
        );
    }

    @WithAuthUser
    @DisplayName("updateMyExecutionData() Success Case1")
    @Test
    void updateExecutionAboutCampaign_SuccessCase1() throws Exception {
        gson = new Gson();
        final String url = "/api/marginforcam/updateExecutionAboutCampaign";

        // Argument Matchers는 stub 메서드에서만 사용해야 합니다.
        doReturn(getMfcValidationResponseDto(2, 2, new ArrayList<>()))
                .when(marginForCampaignService)
                .searchMarginForCampaignProductName(any(String.class), any(MfcRequestDtos.class));

        final MfcRequestDtos requestBody = getRequestDtos(1L, List.of(
                getMfcDto("빨강색", 1L, 2L, 1L, 1.1),
                getMfcDto("파랑색", 2L, 2L, 1L, 1.1)
        ));

        final ResultActions result = mockMvc.perform(patch(url)
                .content(gson.toJson(requestBody))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));

        result.andExpectAll(
                status().isOk(),
                jsonPath("data.requestNumber").value("2"),
                jsonPath("data.responseNumber").value("2")
        );
    }

    @WithAuthUser
    @DisplayName("deleteExecutionAboutCampaign() Success Case")
    @Test
    void deleteExecutionAboutCampaign_Success() throws Exception {
        Long campaignIdToDelete = 1L;
        String url = "/api/marginforcam/deleteExecutionAboutCampaign?id=" + campaignIdToDelete;

        // void 로 건 메소드 when 걸고 싶을때
        doNothing().when(marginForCampaignService).deleteMarginForCampaign(campaignIdToDelete);

        ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .delete(url)
                .with(csrf()));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success : delete"));
    }
    @WithAuthUser
    @DisplayName("getMyAllExecution() -  successCase")
    @Test
    void getMyAllExecution_success() throws Exception {
        String url = "/api/marginforcam/getMyAllExecution";

        List<MarginForCampaignResDto> mockData = List.of(
                createMarginForCampaignResDto("상품1", 1000L),
                createMarginForCampaignResDto("상품2", 2000L)
        );

        // when
        when(marginForCampaignService.getMyAllExecution(any(String.class))).thenReturn(mockData);

        // then
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(url)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].mfcProductName").value("상품1"))
                .andExpect(jsonPath("$.data[0].mfcTotalPrice").value(1000L))
                .andExpect(jsonPath("$.data[1].mfcProductName").value("상품2"))
                .andExpect(jsonPath("$.data[1].mfcTotalPrice").value(2000L));
    }

    @WithAuthUser
    @DisplayName("deleteExecutionAboutCampaign() Fail Case - ID Not Found")
    @Test
    void deleteExecutionAboutCampaign_Fail_IdNotFound() throws Exception {
        Long campaignIdToDelete = 1L;
        String url = "/api/marginforcam/deleteExecutionAboutCampaign?id=" + campaignIdToDelete;

        doThrow(new MarginForCampaignIdNotFoundException()).when(marginForCampaignService).deleteMarginForCampaign(campaignIdToDelete);

        ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .delete(url)
                .with(csrf()));

        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("없는 ID 입니다."));
    }

    private MarginForCampaignResDto createMarginForCampaignResDto(String productName, Long totalPrice) {
        return MarginForCampaignResDto.builder()
                .mfcProductName(productName)
                .mfcTotalPrice(totalPrice)
                .mfcCostPrice(7000L)
                .mfcPerPiece(3L)
                .mfcZeroRoas(1.2)
                .build();
    }
    public MfcRequestDtos getRequestDtos(Long id, List<MfcDto> data) {
        return MfcRequestDtos.builder()
                .campaignId(id)
                .data(data)
                .build();
    }
    public MfcDto getMfcDto(String mfcProductName,Long mfcTotalPrice,Long mfcCostPrice,Long mfcPerPiece,Double mfcZeroRoas) {
        return MfcDto.builder()
                .mfcProductName(mfcProductName)
                .mfcTotalPrice(mfcTotalPrice)
                .mfcCostPrice(mfcCostPrice)
                .mfcPerPiece(mfcPerPiece)
                .mfcZeroRoas(mfcZeroRoas)
                .build();
    }

    private MfcValidationResponseDto getMfcValidationResponseDto(int request, int response, List<String> ProductName) {
        return MfcValidationResponseDto.builder()
                .requestNumber(request)
                .responseNumber(response)
                .failedProductNames(ProductName)
                .build();
    }
}