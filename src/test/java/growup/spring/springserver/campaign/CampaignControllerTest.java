package growup.spring.springserver.campaign;

import com.nimbusds.jose.shaded.gson.Gson;
import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.campaign.controller.CampaignController;
import growup.spring.springserver.campaign.dto.CampaignResponseDto;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.global.config.JwtTokenProvider;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

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
    void test3() throws Exception {
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
    void test4() throws Exception {
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

    public CampaignResponseDto getCampaignResDto(String name,Long id){
        return CampaignResponseDto.builder()
                .title(name)
                .campaignId(id)
                .build();

    }
}
