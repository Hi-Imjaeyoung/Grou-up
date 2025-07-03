package growup.spring.springserver.campaignoptiondetails.controller;

import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.campaignoptiondetails.dto.CampaignOptionDetailsResponseDto;
import growup.spring.springserver.campaignoptiondetails.service.CampaignOptionDetailsService;
import growup.spring.springserver.global.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(CampaignOptionDetailsController.class)
class CampaignOptionDetailsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CampaignOptionDetailsService campaignOptionDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @ParameterizedTest
    @DisplayName("getCampaignDetails() : 시작, 끝 날짜 관련 오류 검증")
    @WithAuthUser
    @CsvSource(value = {"/api/cod/getMyCampaignDetails?campaignId=1&startDate=2025-06-10&endDate=2024-06-11"
            ,"/api/cod/getMyCampaignDetails?campaignId=1&startDate=2025-06-10&endDate=123"})
    void test(String url) throws Exception {
        ResultActions resultActions = mockMvc.perform(get(url)
                .with(csrf()));
        resultActions.andExpectAll(
                status().isOk()
        ).andDo(print());
    }

    @ParameterizedTest
    @MethodSource("invalidGetCampaignDetailsUrl")
    @DisplayName("getCampaignDetails() : campaignId누락")
    @WithAuthUser
    void test3(String url) throws Exception {
        ResultActions resultActions = mockMvc.perform(get(url)
                .with(csrf()));
        resultActions.andExpectAll(
                status().isBadRequest()
        ).andDo(print());
    }

    static Stream<String> invalidGetCampaignDetailsUrl() {
        return Stream.of(
                "/api/cod/getMyCampaignDetails?start=2025-06-10&end=2026-06-11",
                "/api/cod/getMyCampaignDetails?campaignId=송보석&start=2025-06-10&end=2026-06-11"
        );
    }

    @DisplayName("getCampaignDetails() : Success")
    @Test
    @WithAuthUser
    void test2() throws Exception {
        final String url = "/api/cod/getMyCampaignDetails?campaignId=1&start=2025-06-10&end=2025-06-11";
        doReturn(List.of(getCampaignOptionDetailsResponseDto()))
                .when(campaignOptionDetailsService).getCampaignDetailsByCampaignsIds(any(LocalDate.class), any(LocalDate.class), any(Long.class));
        //given
        ResultActions resultActions = mockMvc.perform(get(url));
        //then
        resultActions.andDo(print());

        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("data").isArray(),
                jsonPath("data[0].name").value("송보석"),
                jsonPath("data[0].copSales").value(10L),
                jsonPath("data[0].copImpressions").value(100L)
        );
    }

    public CampaignOptionDetailsResponseDto getCampaignOptionDetailsResponseDto() {
        return CampaignOptionDetailsResponseDto.builder()
                .id(1L)
                .name("송보석")
                .copSales(10L)
                .copImpressions(100L)
                .copAdcost(50.0)
                .copAdsales(200.0)
                .copRoas(400.0)
                .copClicks(20L)
                .copClickRate(20.0)
                .copCvr(10.0)
                .build();
    }
}