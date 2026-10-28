package growup.spring.springserver.marginforcampaign.controller;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import growup.spring.springserver.marginforcampaign.service.ExcelDownloadService;
import growup.spring.springserver.marginforcampaign.service.ExcelParsingService;
import growup.spring.springserver.marginforcampaign.service.ExcelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelUploadStatusTest {

    @Mock
    ExcelService excelService;
    @Mock
    ExcelParsingService excelParsingService;
    @Mock
    ExcelDownloadService excelDownloadService;

    private final UserDetails user = new User("test@test.com", "", List.of());
    private final MockMultipartFile file = new MockMultipartFile("file", "upload.xlsx", null, new byte[]{1});

    private HttpStatus uploadStatusWhenParsingThrows(RuntimeException e) {
        when(excelParsingService.parsingExcelData(any(), anyString())).thenThrow(e);
        ExcelDownUpLoadController controller =
                new ExcelDownUpLoadController(excelService, excelParsingService, excelDownloadService);
        return (HttpStatus) controller.uploadUsersExcel(file, user).getStatusCode();
    }

    @Test
    @DisplayName("파일 형식 오류는 400을 반환한다")
    void invalidFileIsBadRequest() {
        assertThat(uploadStatusWhenParsingThrows(new GrouException(ErrorCode.FILE_INVALID_DATA_FORM)))
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("내 캠페인이 아닌 캠페인 ID가 있으면 오류 코드의 상태(404)를 반환한다")
    void unknownCampaignIsNotFound() {
        assertThat(uploadStatusWhenParsingThrows(new GrouException(ErrorCode.CAMPAIGN_NOT_FOUND)))
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("예상하지 못한 서버 오류는 500을 반환한다")
    void unexpectedErrorIsServerError() {
        assertThat(uploadStatusWhenParsingThrows(new IllegalStateException("DB down")))
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
