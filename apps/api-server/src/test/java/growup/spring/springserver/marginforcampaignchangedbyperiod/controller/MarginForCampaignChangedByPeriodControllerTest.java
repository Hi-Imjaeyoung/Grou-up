package growup.spring.springserver.marginforcampaignchangedbyperiod.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.global.config.GsonConfig;
import growup.spring.springserver.global.config.JwtTokenProvider;
import growup.spring.springserver.marginforcampaignchangedbyperiod.dto.MarginChangeSaveRequestDto;
import growup.spring.springserver.marginforcampaignchangedbyperiod.service.MarginForCampaignChangedByPeriodService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(MarginForCampaignChangedByPeriodController.class)
class MarginForCampaignChangedByPeriodControllerTest {
    @Autowired
    private MockMvc mockMvc;


    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private MarginForCampaignChangedByPeriodService marginForCampaignChangedByPeriodService;

    @WithAuthUser
    @Test
    @DisplayName("save() 성공")
    void getExecutionAboutCampaign_Fail() throws Exception {
        Gson gson = GsonConfig.getGson();


        final String url = "/api/marginforcampaignchangedbyperiod/save";
        MarginChangeSaveRequestDto dto = dto(1L,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 4),
                1000L,
                2000L,
                500L,
                100L);

        mockMvc.perform(patch(url)
                .content(gson.toJson(dto))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
                )
        .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success: marginforcampaignchangedbyperiodsave"))
                .andExpect(jsonPath("$.data").value("marginforcampaignchangedbyperiod update successful"));
    }
    private MarginChangeSaveRequestDto dto(Long mfcId, LocalDate startDate, LocalDate endDate,
                                           Long salePrice, Long totalPrice, Long costPrice, Long returnPrice) {
        return new MarginChangeSaveRequestDto(
                mfcId,
                startDate,
                endDate,
                salePrice,
                totalPrice,
                costPrice,
                returnPrice
        );
    }
}