package growup.spring.springserver.marginforcampaign.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.dto.ExcelUploadContext;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ExcelParsingServiceTest {

    private static final String EMAIL = "test@test.com";

    @Mock
    MarginForCampaignRepository marginForCampaignRepository;
    @Mock
    CampaignService campaignService;

    private ExcelParsingService excelParsingService;
    private Campaign campaign1;
    private Campaign campaign2;

    @BeforeEach
    void setUp() {
        excelParsingService = new ExcelParsingService(marginForCampaignRepository, campaignService);
        campaign1 = Campaign.builder().campaignId(1L).camCampaignName("캠페인1").build();
        campaign2 = Campaign.builder().campaignId(2L).camCampaignName("캠페인2").build();
        lenient().when(campaignService.getCampaignsByEmail(anyString())).thenReturn(List.of(campaign1, campaign2));
        lenient().when(marginForCampaignRepository.findByCampaignMemberEmail(anyString())).thenReturn(List.of());
    }

    @Test
    @DisplayName("행의 캠페인 ID 열로 캠페인을 찾아 연결한다")
    void usesCampaignIdColumn() throws IOException {
        ExcelUploadContext context = excelParsingService.parsingExcelData(excel(
                row(1L, "옵션A"),
                row(2L, "옵션B")), EMAIL);

        assertThat(context.getEntitiesToInsert())
                .extracting(mfc -> mfc.getCampaign().getCampaignId(), MarginForCampaign::getMfcProductName)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(1L, "옵션A"),
                        org.assertj.core.groups.Tuple.tuple(2L, "옵션B"));
    }

    @Test
    @DisplayName("엑셀에 있는 옵션명을 캠페인별로 모아 기존 옵션 정리에 쓴다")
    void collectsOptionNamesPerCampaign() throws IOException {
        ExcelUploadContext context = excelParsingService.parsingExcelData(excel(
                row(1L, "옵션A"),
                row(1L, "옵션B"),
                row(2L, "옵션C")), EMAIL);

        assertThat(context.getOptionNamesFromExcel().get(1L)).containsExactlyInAnyOrder("옵션A", "옵션B");
        assertThat(context.getOptionNamesFromExcel().get(2L)).containsExactly("옵션C");
    }

    @Test
    @DisplayName("내 캠페인이 아닌 캠페인 ID가 있으면 업로드 전체를 CAMPAIGN_NOT_FOUND로 중단한다")
    void unknownCampaignAbortsUpload() throws IOException {
        byte[] file = excelBytes(row(1L, "옵션A"), row(99L, "옵션B"));

        assertThatThrownBy(() -> excelParsingService.parsingExcelData(multipart(file), EMAIL))
                .isInstanceOf(GrouException.class)
                .extracting(e -> ((GrouException) e).getErrorCode())
                .isEqualTo(ErrorCode.CAMPAIGN_NOT_FOUND);
    }

    @Test
    @DisplayName("중간에 빈 행이 있어도 나머지 행을 처리한다")
    void skipsBlankRows() throws IOException {
        ExcelUploadContext context = excelParsingService.parsingExcelData(excel(
                row(1L, "옵션A"),
                null,
                row(1L, "옵션B")), EMAIL);

        assertThat(context.getEntitiesToInsert()).extracting(MarginForCampaign::getMfcProductName)
                .containsExactlyInAnyOrder("옵션A", "옵션B");
        assertThat(context.getResult().get("total")).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 캠페인에 같은 옵션명이 두 번 있으면 한 번만 INSERT 하고 마지막 값을 쓴다")
    void duplicateOptionNameInsertsOnce() throws IOException {
        ExcelUploadContext context = excelParsingService.parsingExcelData(excel(
                row(1L, "옵션A", 100L),
                row(1L, "옵션A", 200L)), EMAIL);

        assertThat(context.getEntitiesToInsert()).hasSize(1);
        assertThat(context.getEntitiesToInsert().get(0).getMfcSalePrice()).isEqualTo(200L);
        assertThat(context.getResult().get("duplicate")).isEqualTo(1);
    }

    @Test
    @DisplayName("엑셀로 읽을 수 없는 파일이면 null 대신 FILE_READ_ERROR를 던진다")
    void unreadableFileThrows() {
        MockMultipartFile broken = new MockMultipartFile("file", "broken.xlsx", null, "not an excel".getBytes());

        assertThatThrownBy(() -> excelParsingService.parsingExcelData(broken, EMAIL))
                .isInstanceOf(GrouException.class)
                .extracting(e -> ((GrouException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_READ_ERROR);
    }

    // 다운로드 양식과 같은 열: 캠페인 ID, 캠페인 명, 옵션명, 판매가, 원가, 총 비용, 반품비
    private Object[] row(Long campaignId, String optionName) {
        return row(campaignId, optionName, 300L);
    }

    private Object[] row(Long campaignId, String optionName, Long salePrice) {
        return new Object[]{campaignId, "캠페인", optionName, salePrice, 50L, 400L, 80L};
    }

    private MockMultipartFile excel(Object[]... rows) throws IOException {
        return multipart(excelBytes(rows));
    }

    private MockMultipartFile multipart(byte[] bytes) {
        return new MockMultipartFile("file", "upload.xlsx", null, bytes);
    }

    private byte[] excelBytes(Object[]... rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            String[] headers = {"캠패인 ID", "캠페인 명", "옵션명", "판매가", "원가", "총 비용(쿠팡)", "반품비"};
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            for (int r = 0; r < rows.length; r++) {
                if (rows[r] == null) continue; // 빈 행
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    Object v = rows[r][c];
                    if (v instanceof Long l) row.createCell(c).setCellValue(l);
                    else row.createCell(c).setCellValue((String) v);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
