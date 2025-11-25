package growup.spring.springserver.marginforcampaign.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.dto.CampaignIdAndNameForExcelDownload;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.global.common.ExcelUtil;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import growup.spring.springserver.marginforcampaign.TypeChangeMarginForCampaign;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.dto.MarginForCampaignOptionNameAndCampaignId;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ExcelService {
    private final MarginForCampaignRepository marginForCampaignRepository;
    private final CampaignService campaignService;
    @Getter
    private static class ExcelUploadContext {
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

        public ExcelUploadContext(int totalRow,String email,
                                  Map<Long, Campaign> campaignCache,
                                  Map<Long,Map<String,MarginForCampaign>> myMFCCache) {
            this.email = email;
            this.campaignCache = campaignCache;
            this.myMFCCache = myMFCCache;
            this.result.put("total", totalRow);
            this.result.put("error", 0);
            this.result.put("update", 0);
            this.result.put("input", 0);
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
        /*
            액셀 다운로드  시 필요한 기본 데인터를 Map 형태로 갖는 List.
         */
        List<Map<String, Object>> dummyUsers = makeMarginForCampaignExcelBasicDataForm(email);
        List<String> headers = List.of("캠패인 ID","캠페인 명", "옵션명", "판매가","원가","총 비용(쿠팡)","반품비");
        List<String> dataKeys = List.of("campaignId","campaignName", "optionName","salePrice","costPrice", "totalPrice","returnPrice");
        return ExcelUtil.createExcelFile(dummyUsers, headers, dataKeys);
    }

    public List<Map<String, Object>> makeMarginForCampaignExcelBasicDataForm(String email){
        /*
            액셀 다운로드  시 필요한 기본 데인터를 Map 형태로 갖는 List.
         */
        List<Map<String, Object>> dummyUsers = new ArrayList<>();
        /*
            기존 사용자가 갖고 있는 MarginForCampaign List
         */
        List<MarginForCampaign> marginForCampaigns =
                marginForCampaignRepository.findByCampaignMemberEmail(email);
        /*
            Key : 캠패인 id , Value : MarginForCampaign
         */
        Map<Long, List<MarginForCampaign>> map = marginForCampaigns.stream()
                .collect(Collectors.groupingBy(
                        mfc -> mfc.getCampaign().getCampaignId()
                ));
        if (marginForCampaigns.isEmpty()) {
            /*
                사용자가 갖는 캠패인들의 id 와 이름 List
             */
            List<CampaignIdAndNameForExcelDownload> campaignNameAndIDList =
                    campaignService.getCampaignsByEmail(email).stream()
                            .map(campaign -> new CampaignIdAndNameForExcelDownload(campaign.getCampaignId(), campaign.getCamCampaignName()))
                            .toList();
            for(CampaignIdAndNameForExcelDownload campaignIdAndNameForExcelDownload : campaignNameAndIDList){
                dummyUsers.add(
                        Map.of("campaignName", campaignIdAndNameForExcelDownload.campaignName(), "campaignId", campaignIdAndNameForExcelDownload.campaignId())
                );
            }
            /*
                캠패인 마저 없는 경우 값을 채워준다.
             */
            if(dummyUsers.isEmpty()) dummyUsers.add(Map.of("campaignName", "캠피인 1", "campaignId", 123456L));
            return dummyUsers;
        }
        for(Long campaignId : map.keySet()){
            for (MarginForCampaign marginForCampaign : map.get(campaignId)) {
                dummyUsers.add(
                        Map.of("campaignName", marginForCampaign.getCampaign().getCamCampaignName(),
                                "campaignId", marginForCampaign.getCampaign().getCampaignId(),
                                "optionName", marginForCampaign.getMfcProductName(),
                                "salePrice", marginForCampaign.getMfcSalePrice(),
                                "costPrice", marginForCampaign.getMfcCostPrice(),
                                "totalPrice", marginForCampaign.getMfcTotalPrice(),
                                "returnPrice", marginForCampaign.getMfcReturnPrice())
                );
            }
        }
        return dummyUsers;
    }
    @Transactional
    public Map<String,Integer> processUploadedExcel(MultipartFile file,String email) throws IOException {
        // N + 1 이슈를 방지하기 위해 캠패인 조회 후 캐싱 처리.
        Map<Long,Campaign> myCampaignCache = campaignService.getCampaignsByEmail(email)
                .stream()
                .collect(Collectors.toMap(
                        Campaign::getCampaignId,
                        campaign -> campaign
                ));
        // N + 1 이슈를 방지하기 위해 MarginForCampaign 조회 후 캐싱처리
        Map<Long,Map<String,MarginForCampaign>> myMFCCache = marginForCampaignRepository.findByCampaignMemberEmail(email)
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
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            ExcelUploadContext excelUploadContext = new ExcelUploadContext(sheet.getLastRowNum(), email, myCampaignCache,myMFCCache);
            processExcelRows(sheet, excelUploadContext);
            if (!excelUploadContext.getOptionNamesFromExcel().isEmpty()) {
                // "엑셀에 없는" 기존 옵션 삭제
                for(Long campaignId : excelUploadContext.getOptionNamesFromExcel().keySet()){
                    if(excelUploadContext.optionNamesFromExcel.get(campaignId).isEmpty()) continue;
                    checkNotExistOptions(excelUploadContext.getOptionNamesFromExcel().get(campaignId), email,campaignId);
                }
            }
            if (!excelUploadContext.getEntitiesToSave().isEmpty()) {
                // 엑셀에 있던 내용 일괄 저장 (Update + Insert)
                marginForCampaignRepository.saveAll(excelUploadContext.getEntitiesToSave());
            }
            return excelUploadContext.getResult();
        }
    }
    private void processExcelRows(Sheet sheet, ExcelUploadContext excelUploadContext) {
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

    public void checkNotExistOptions(List<String> optionNamesInExcels, String email,Long campaignId){
        marginForCampaignRepository.deleteNotIncludeOptionName(optionNamesInExcels,email,campaignId);
    }

    public MfcDto readExcelRowToMarginForCampaignData(Row row){
        String optionName = getCellStringValue(row.getCell(2));
        Long salePrice = convertStringToLong(getCellStringValue(row.getCell(3)));
        Long costPrice = convertStringToLong(getCellStringValue(row.getCell(4)));
        Long totalPrice = convertStringToLong(getCellStringValue(row.getCell(5)));
        Long returnPrice = convertStringToLong(getCellStringValue(row.getCell(6)));
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
}
