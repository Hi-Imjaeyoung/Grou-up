package growup.spring.springserver.file.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import growup.spring.springserver.annotation.WithAuthUser;
import growup.spring.springserver.file.FileType;
import growup.spring.springserver.global.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import growup.spring.springserver.file.controller.FileController;
import growup.spring.springserver.file.dto.FileResDto;
import growup.spring.springserver.file.dto.FileResponseDto;
import growup.spring.springserver.file.service.FileService;

@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

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
        LocalDate start = LocalDate.of(2024, 11, 11);
        LocalDate end   = LocalDate.of(2024, 11, 13);
        FileType fileType = FileType.ADVERTISING_REPORT;

        Map<LocalDate, List<FileResponseDto>> advMap = Map.of(
                LocalDate.of(2024,11,11),
                List.of(
                        FileResponseDto.builder()
                                .id(1L)
                                .fileName("file1.txt")
                                .fileUploadDate(LocalDate.of(2024,11,11))
                                .build()
                ),
                LocalDate.of(2024,11,12),
                List.of(
                        FileResponseDto.builder()
                                .id(3L)
                                .fileName("file3.txt")
                                .fileUploadDate(LocalDate.of(2024,11,12))
                                .build()
                )
        );
        Map<LocalDate, List<FileResponseDto>> netMap = Map.of(
                LocalDate.of(2024,11,11),
                List.of(
                        FileResponseDto.builder()
                        .id(2L)
                        .fileName("file2.txt")
                        .fileUploadDate(LocalDate.of(2024,11,11))
                        .build()
                )
        );
        FileResDto mockDto = FileResDto.builder()
                .advertisingReport(advMap)
                .netSalesReport(netMap)
                .build();

        given(fileService.getFileHistory(email, start, end))
                .willReturn(mockDto);

        mockMvc.perform(get("/api/file/getHistory")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success :getFileHistory"))
                .andExpect(jsonPath("$.data.advertisingReport").isNotEmpty())
                .andExpect(jsonPath("$.data.advertisingReport['2024-11-11'][0].id").value(1L))
                .andExpect(jsonPath("$.data.advertisingReport['2024-11-12'][0].id").value(3L));

    }

    @DisplayName("getFileHistory() : empty data returns empty maps")
    @Test
    @WithAuthUser
    void getFileHistory_emptyData() throws Exception {
        // given
        String email = "test@test.com";
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end   = LocalDate.of(2024, 1, 1);
        FileType fileType = FileType.ADVERTISING_REPORT;

        FileResDto emptyDto = FileResDto.builder()
                .advertisingReport(Collections.emptyMap())
                .netSalesReport(Collections.emptyMap())
                .build();

        given(fileService.getFileHistory(email, start, end))
                .willReturn(emptyDto);

        // when & then
        mockMvc.perform(get("/api/file/getHistory")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success :getFileHistory"))
                // 빈 오브젝트(맵)인지 확인
                .andExpect(jsonPath("$.data.advertisingReport").isEmpty())
                .andExpect(jsonPath("$.data.netSalesReport").isEmpty());
    }
}
