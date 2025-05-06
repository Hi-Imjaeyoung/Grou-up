package growup.spring.springserver.file.controller;

import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.global.config.JwtTokenProvider;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import growup.spring.springserver.file.FileType;
import growup.spring.springserver.file.dto.FileResDto;
import growup.spring.springserver.file.service.FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    FileService fileService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;


    @DisplayName("getFileHistory() : successCase")
    @Test
    @WithAuthUser
    void getFileHistory_successCase() throws Exception {
        // given
        String email = "test@test.com";
        FileType fileType = FileType.ADVERTISING_REPORT;
        FileResDto mockFileResDto = new FileResDto(1L, "testFile", LocalDateTime.now(), 10L, 5L, 2L);
        FileResDto mockFileResDto2 = new FileResDto(2L, "testFile2", LocalDateTime.now(), 10L, 5L, 2L);

        when(fileService.getFileHistory(fileType, email))
                .thenReturn(List.of(mockFileResDto,mockFileResDto2));

        // when & then
        mockMvc.perform(get("/api/file/history")
                        .param("fileType", fileType.name())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success :getFileHistory"))
                .andExpect(jsonPath("$.data[0].fileName").value("testFile"))
                .andExpect(jsonPath("$.data[0].id").value(1L))

                .andExpect(jsonPath("$.data[1].fileName").value("testFile2"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].fileAllCount").value(10))
                .andExpect(jsonPath("$.data[1].fileNewCount").value(5))
                .andExpect(jsonPath("$.data[1].fileDuplicateCount").value(2));

    }

    @DisplayName("getFileHistory() : failCase1. 파일 내역이 없는 경우")
    @Test
    @WithAuthUser
    void getFileHistory_failCase() throws Exception {
        // given
        String email = "test@example.com";
        FileType fileType = FileType.ADVERTISING_REPORT;

        when(fileService.getFileHistory(fileType, email))
                .thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/file/history")
                        .param("fileType", fileType.name())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success :getFileHistory"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @DisplayName("getFileHistory() : failCase2. 잘못된 fileType 파라미터로 400 오류 발생")
    @Test
    @WithAuthUser
    void getFileHistory_invalidFileType() throws Exception {

        mockMvc.perform(get("/api/file/history")
                        .param("fileType", "INVALID_TYPE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}