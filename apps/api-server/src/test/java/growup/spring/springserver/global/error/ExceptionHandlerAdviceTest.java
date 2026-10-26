package growup.spring.springserver.global.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// DummyControllers에 API 엔드포인트로 호출하여 에러를 터트린다.
class ExceptionHandlerAdviceTest {
    private MockMvc mockMvc;

    private void setUpMockMvc(Object controller) {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ExceptionHandlerAdvice())
                .build();
    }

    @Test
    @DisplayName("CampaignNotFoundException : 400 BAD_REQUEST")
    void campaignNotFoundException_returns400() throws Exception {
        setUpMockMvc(new DummyControllers.CampaignNotFoundController());

        mockMvc.perform(get("/test/campaignNotFound")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.errorMessage").value("현재 등록된 캠페인이 없습니다."));
    }

    @Test
    @DisplayName("RequestError : 400 BAD_REQUEST")
    void requestError_returns400() throws Exception {
        setUpMockMvc(new DummyControllers.RequestErrorController());

        mockMvc.perform(get("/test/requestError")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.errorMessage").value("잘못된 요청값 입니다."));
    }

    @Test
    @DisplayName("InvalidDateFormatException : 200 OK + 빈 리스트 반환")
    void invalidDateFormatException_returnsEmptyList() throws Exception {
        setUpMockMvc(new DummyControllers.InvalidDateFormatController());

        mockMvc.perform(get("/test/invalidDateFormat")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("요청 날짜 형식 오류로 인한 빈 값 리턴."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
