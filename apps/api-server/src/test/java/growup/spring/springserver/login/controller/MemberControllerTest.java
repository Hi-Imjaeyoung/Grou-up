package growup.spring.springserver.login.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.global.config.JwtTokenProvider;
import growup.spring.springserver.login.dto.response.LoginDataResDto;
import growup.spring.springserver.login.service.MemberService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(MemberController.class)
public class MemberControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("getMyEmailAndRole : ErrorCase1.인가되지 않은 사용자 접근")
    void test1() throws Exception {
        final String url = "/api/members/getMyName";
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(url));
        resultActions.andDo(print()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("getMyEmailAndRole : Success1.접근 성공")
    @WithAuthUser
    void test2() throws Exception {
        Gson gson = new Gson();
        final String url = "/api/members/getMyName";

        LoginDataResDto mockResponse = new LoginDataResDto("fa7271@naver.com");
        doReturn(mockResponse).when(memberService).getMyName(any());

        // MockMvc 요청
        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(new ArrayList<String>())));

        // 응답 검증
        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("message").value("getMyEmailAndRole Success"), // "message" 필드 검증
                jsonPath("data.name").value("fa7271@naver.com")
        ).andDo(print());
    }
    @Test
    @DisplayName("getMySun() : SuccessCase")
    @WithAuthUser
    void test3() throws Exception {
        Gson gson = new Gson();

        final String url = "/api/members/getMySun";

        Long Response = 10L;
        doReturn(Response).when(memberService).getMySun(any());

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(new ArrayList<String>())));

        resultActions.andExpectAll(
                status().isOk(),
                jsonPath("message").value("getMySun Success"),
                jsonPath("data").value(Response)
        ).andDo(print());
    }
}
