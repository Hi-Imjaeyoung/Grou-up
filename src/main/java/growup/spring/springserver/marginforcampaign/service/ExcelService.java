package growup.spring.springserver.marginforcampaign.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.dto.CampaignIdAndNameForExcelDownload;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.global.common.ExcelUtil;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import growup.spring.springserver.marginforcampaign.TypeChangeMarginForCampaign;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.dto.MfcDto;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
@AllArgsConstructor
public class ExcelService {
    private final MarginForCampaignRepository marginForCampaignRepository;
    private final CampaignService campaignService;
    @Getter
    private class ExcelUploadContext {
        /*
         업로드 결과를 저장하는 Map
         Key : insert(새로 추가된 옵션), update(기존 옵션), error(파일 형식 오류), total(전체 옵션)
         */
        private final Map<String,Integer> result = new HashMap<>();
        /*
         저장할 MFC 모아두는 List
         */
        private final List<MarginForCampaign> entitiesToSave = new ArrayList<>();
        /*
         사용자가 올린 엑셀의 옵션명들을 value, campaignId를 key 로 갖는 Map
         */
        private final Map<Long,List<String>> optionNamesFromExcel = new HashMap<>();

        private final String email;
        private final Map<Long, Campaign> campaignCache;
        private final Map<Long,Map<String,MarginForCampaign>> myMFCCache;

        public ExcelUploadContext(int totalRow,String email) {
            this.email = email;
            this.campaignCache = loadMyCampaignCache(email);
            this.myMFCCache = loadMyMFCCache(email);
            this.result.put("total", totalRow);
            this.result.put("error", 0);
            this.result.put("update", 0);
            this.result.put("input", 0);
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
        public void increment(String key) {
            result.put(key, result.get(key) + 1);
        }

        public void addEntityToSave(MarginForCampaign entity) {
            this.entitiesToSave.add(entity);
        }

        public void addOptionNameFromExcel(Long campaignId,String name) {
            this.optionNamesFromExcel.computeIfAbsent(campaignId, k -> new ArrayList<>());
            this.optionNamesFromExcel.get(campaignId).add(name);
        }
        public boolean existMfcCacheMayToCampaignId(Long campaignId){
            return myMFCCache.containsKey(campaignId);
        }
        public MarginForCampaign getMfcToCampaignIdAndProductNameOrNull(Long campaignId,String productName){
            if(!existMfcCacheMayToCampaignId(campaignId)){
                return null;
            }
            return this.myMFCCache.get(campaignId).get(productName);
        }
        public void upsertMfcDtoToEntitiesToSave(MarginForCampaign oldMFc,MfcDto mfcDto,Campaign campaign){
            if(oldMFc == null){
                addEntityToSave(
                        TypeChangeMarginForCampaign.createDtoToMargin(mfcDto, campaign)
                );
                increment("input");
            }else{
                oldMFc.updateExistingProduct(mfcDto);
                addEntityToSave(oldMFc);
                increment("update");
            }
        }
        public Campaign getCampaignToCampaignIdInCampaignCache(Long campaignId){
            try {
                return campaignCache.get(campaignId);
            } catch (NullPointerException e){
                // 이 캠페인 ID가 엑셀에는 있는데, 내 DB에 없거나 내 소유가 아님
                throw new GrouException(ErrorCode.CAMPAIGN_NOT_FOUND);
            }
        }

    }
    public Workbook createUsersExcel(String email){
        List<Map<String, Object>> writtenDataToDownloadExcel = makeMarginForCampaignExcelBasicDataForm(email);
        List<String> headers = List.of("캠패인 ID","캠페인 명", "옵션명", "판매가","원가","총 비용(쿠팡)","반품비");
        List<String> dataKeys = List.of("campaignId","campaignName", "optionName","salePrice","costPrice", "totalPrice","returnPrice");
        return ExcelUtil.createExcelFile(writtenDataToDownloadExcel, headers, dataKeys);
    }
    public List<Map<String, Object>> makeMarginForCampaignExcelBasicDataForm(String email){
        List<MarginForCampaign> myMarginForCampaigns =
                marginForCampaignRepository.findByCampaignMemberEmail(email);
        if (myMarginForCampaigns.isEmpty()) {
            return getMinimalDataAboutCampaignNameAndIDToWrittenExcel(email);
        }
        return getMyMFCDataToWrittenExcel(myMarginForCampaigns);

    }
    public List<Map<String, Object>> getMyMFCDataToWrittenExcel(List<MarginForCampaign> myMarginForCampaigns){
        return myMarginForCampaigns.stream()
                .sorted(Comparator.comparing(mfc -> mfc.getCampaign().getCampaignId()))
                .map(mfc -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("campaignName", mfc.getCampaign().getCamCampaignName());
                    map.put("campaignId", mfc.getCampaign().getCampaignId());
                    map.put("optionName", mfc.getMfcProductName());
                    map.put("salePrice", mfc.getMfcSalePrice());
                    map.put("costPrice", mfc.getMfcCostPrice());
                    map.put("totalPrice", mfc.getMfcTotalPrice());
                    map.put("returnPrice", mfc.getMfcReturnPrice());
                    return map;
                })
                .collect(Collectors.toList());
    }
    public List<Map<String, Object>> getMinimalDataAboutCampaignNameAndIDToWrittenExcel(String email){
        List<Map<String, Object>> writtenAboutCampaignNameAndID = new ArrayList<>();
        List<CampaignIdAndNameForExcelDownload> campaignNameAndIDList =
                campaignService.getCampaignsByEmail(email).stream()
                        .map(campaign -> new CampaignIdAndNameForExcelDownload(campaign.getCampaignId(), campaign.getCamCampaignName()))
                        .toList();
        for(CampaignIdAndNameForExcelDownload campaignIdAndNameForExcelDownload : campaignNameAndIDList){
            writtenAboutCampaignNameAndID.add(
                    Map.of("campaignName", campaignIdAndNameForExcelDownload.campaignName(), "campaignId", campaignIdAndNameForExcelDownload.campaignId())
            );
        }
        if(writtenAboutCampaignNameAndID.isEmpty()) writtenAboutCampaignNameAndID.add(Map.of("campaignName", "캠피인 1", "campaignId", 123456L));
        return writtenAboutCampaignNameAndID;
    }

    @Transactional
    public Map<String,Integer> processUploadedExcel(MultipartFile file,String email) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            ExcelUploadContext excelUploadContext = new ExcelUploadContext(sheet.getLastRowNum(), email);
            readAllExcelRows(sheet, excelUploadContext);
            deleteOldMfcNotExistExcel(excelUploadContext,email);
            saveMfcInExcel(excelUploadContext);
            return excelUploadContext.getResult();
        }
    }
    private void readAllExcelRows(Sheet sheet, ExcelUploadContext excelUploadContext) {
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            try {
                processSingleRow(row, excelUploadContext);
            } catch (GrouException e) {
                handleRowProcessingError(e, excelUploadContext);
            }
        }
    }
    private void processSingleRow(Row row, ExcelUploadContext excelUploadContext) {
        MfcDto mfcDto = readExcelRowToMarginForCampaignData(row);
        Long campaignID = Long.valueOf(getCellStringValue(row.getCell(0)));
        Campaign campaign = excelUploadContext.getCampaignToCampaignIdInCampaignCache(campaignID);
        excelUploadContext.addOptionNameFromExcel(campaign.getCampaignId(),mfcDto.getMfcProductName());
        MarginForCampaign oldMFc =
                excelUploadContext.getMfcToCampaignIdAndProductNameOrNull(campaign.getCampaignId(),mfcDto.getMfcProductName());
        excelUploadContext.upsertMfcDtoToEntitiesToSave(oldMFc,mfcDto,campaign);
    }
    public MfcDto readExcelRowToMarginForCampaignData(Row row){
        String optionName = getCellStringValue(row.getCell(2));
        Long salePrice = convertStringToLong(getCellStringValue(row.getCell(3)));
        Long costPrice = convertStringToLong(getCellStringValue(row.getCell(4)));
        Long totalPrice = convertStringToLong(getCellStringValue(row.getCell(5)));
        Long returnPrice;
        try{
            returnPrice =  convertStringToLong(getCellStringValue(row.getCell(6)));
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
    private String getCellStringValue(Cell cell) {
        // 빈 값을 넣었을 경우, 예외를 던짐
        if (cell == null) {
            throw new GrouException(ErrorCode.FILE_INVALID_DATA_FORM);
        }
        return switch (cell.getCellType()) {
            case STRING, FORMULA -> cell.getStringCellValue();
            case NUMERIC -> {
                DataFormatter formatter = new DataFormatter();
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
    public void checkNotExistOptionsAndDelete(List<String> optionNamesInExcels, String email,Long campaignId){
        marginForCampaignRepository.deleteNotIncludeOptionName(optionNamesInExcels,email,campaignId);
    }
    private void deleteOldMfcNotExistExcel(ExcelUploadContext excelUploadContext,String email){
        if (excelUploadContext.getOptionNamesFromExcel().isEmpty()) {
            return;
        }
        for(Long campaignId : excelUploadContext.getOptionNamesFromExcel().keySet()){
            if(excelUploadContext.optionNamesFromExcel.get(campaignId).isEmpty()) continue;
            checkNotExistOptionsAndDelete(excelUploadContext.getOptionNamesFromExcel().get(campaignId), email,campaignId);
        }
    }
    private void saveMfcInExcel(ExcelUploadContext excelUploadContext){
        if (excelUploadContext.getEntitiesToSave().isEmpty()) {
            return;
        }
        marginForCampaignRepository.saveAll(excelUploadContext.getEntitiesToSave());
    }
}
