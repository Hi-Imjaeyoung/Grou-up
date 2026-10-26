package growup.spring.springserver.marginforcampaign.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.dto.ExcelUploadContext;
import growup.spring.springserver.marginforcampaign.dto.MfcDto;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ExcelParsingService {
    private final MarginForCampaignRepository marginForCampaignRepository;
    private final CampaignService campaignService;

    public ExcelUploadContext parsingExcelData(MultipartFile file, String email) {

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            ExcelUploadContext excelUploadContext = new ExcelUploadContext(sheet.getLastRowNum(),email,loadMyCampaignCache(email),loadMyMFCCache(email));
            readAllExcelRows(sheet, excelUploadContext);
            return excelUploadContext;
        } catch (IOException e){

            return null;
        }
    }
    private Map<Long, Campaign> loadMyCampaignCache(String email){
        return campaignService.getCampaignsByEmail(email)
                .stream()
                .collect(Collectors.toMap(
                        Campaign::getCampaignId,
                        campaign -> campaign
                ));
    }

    private Map<Long,Map<String,MarginForCampaign>> loadMyMFCCache(String email){
        return marginForCampaignRepository.findByCampaignMemberEmail(email)
                .stream()
                .collect(Collectors.groupingBy(
                                mfc -> mfc.getCampaign().getCampaignId(),
                                Collectors.toMap(
                                        MarginForCampaign::getMfcProductName, // Key = 상품명
                                        Function.identity(),                  // Value = mfc 객체
                                        (existingValue, replacementValue) -> existingValue
                                )
                        )
                );
    }
    private void readAllExcelRows(Sheet sheet, ExcelUploadContext excelUploadContext) {
        DataFormatter formatter = new DataFormatter();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            try {
                processSingleRow(row, excelUploadContext,formatter);
            } catch (GrouException e) {
                handleRowProcessingError(e, excelUploadContext);
            }
        }
    }
    private void processSingleRow(Row row, ExcelUploadContext excelUploadContext,DataFormatter formatter) {
        MfcDto mfcDto = readExcelRowToMarginForCampaignData(row,formatter);
//        Long campaignID = Long.valueOf(getCellStringValue(row.getCell(0),formatter));
//        Campaign campaign = excelUploadContext.getCampaignToCampaignIdInCampaignCache(campaignID);
//        excelUploadContext.addOptionNameFromExcel(campaign.getCampaignId(),mfcDto.getMfcProductName());

        Campaign campaign = excelUploadContext.getRandomCampaign();
        MarginForCampaign oldMFc =
                excelUploadContext.getMfcToCampaignIdAndProductNameOrNull(campaign.getCampaignId(),mfcDto.getMfcProductName());
        excelUploadContext.upsertMfcDtoToEntitiesToSave(oldMFc,mfcDto,campaign);
    }
    public MfcDto readExcelRowToMarginForCampaignData(Row row,DataFormatter formatter){
        String optionName = getCellStringValue(row.getCell(2),formatter);
        Long salePrice = convertStringToLong(getCellStringValue(row.getCell(3),formatter));
        Long costPrice = convertStringToLong(getCellStringValue(row.getCell(4),formatter));
        Long totalPrice = convertStringToLong(getCellStringValue(row.getCell(5),formatter));
        Long returnPrice;
        try{
            returnPrice =  convertStringToLong(getCellStringValue(row.getCell(6),formatter));
        } catch (GrouException e) {
            returnPrice = 0L;
        }
        return MfcDto.builder()
                .mfcType(MarginType.ROCKET_GROWTH)
                .mfcProductName(optionName)
                .mfcCostPrice(costPrice)
                .mfcSalePrice(salePrice)
                .mfcTotalPrice(totalPrice)
                .mfcReturnPrice(returnPrice)
                .build();
    }
    public Long convertStringToLong(String input){
        try {
            return Long.parseLong(input);
        }catch (NumberFormatException e){
            throw new GrouException(ErrorCode.FILE_INVALID_DATA_FORM);
        }
    }
    private String getCellStringValue(Cell cell,DataFormatter formatter) {
        // 빈 값을 넣었을 경우, 예외를 던짐
        if (cell == null) {
            throw new GrouException(ErrorCode.FILE_INVALID_DATA_FORM);
        }
        return switch (cell.getCellType()) {
            case STRING, FORMULA -> cell.getStringCellValue();
            case NUMERIC -> {
                yield formatter.formatCellValue(cell);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
    private void handleRowProcessingError(GrouException e, ExcelUploadContext excelUploadContext) {
        if (e.getErrorCode().equals(ErrorCode.FILE_INVALID_DATA_FORM)) {
            // 잘못된 행(Row)은 "error" 카운트만 올리고 다음 행으로 넘어간다.
            excelUploadContext.increment("error");
        } else {
            // CAMPAIGN_NOT_FOUND 등... "치명적인" 에러는
            // 엑셀 업로드 전체를 중단시켜야 하므로, 예외를 다시 던진다.
            log.error("MFC 액셀 업로드 중 치명적 에러 발생 : {}", e.getErrorCode().getMessage());
            throw e;
        }
    }
}
