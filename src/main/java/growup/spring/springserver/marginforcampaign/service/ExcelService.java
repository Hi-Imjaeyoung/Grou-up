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
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ExcelService {
    private final MarginForCampaignRepository marginForCampaignRepository;
    private final CampaignService campaignService;

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
            Key : 캠패인 Id , Value : MarginForCampaign
         */
        Map<Long, List<MarginForCampaign>> map = marginForCampaigns.stream()
                .collect(Collectors.groupingBy(
                        mfc -> mfc.getCampaign().getCampaignId()
                ));
        if (marginForCampaigns.isEmpty()) {
            /*
                사용자가 갖는 캠패인들의 id 와 이름 List
             */
            List<CampaignIdAndNameForExcelDownload> campaignList =
                    campaignService.getCampaignsByEmail(email).stream()
                            .map(campaign -> new CampaignIdAndNameForExcelDownload(campaign.getCampaignId(), campaign.getCamCampaignName()))
                            .toList();
            for(CampaignIdAndNameForExcelDownload campaignIdAndNameForExcelDownload : campaignList){
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

    public Map<String,Integer> processUploadedExcel(MultipartFile file,String email) throws IOException {
        /*
            액셀 업로드 결과를 사용자에게 전달하기 위한 Map.
            key : 업로드 결과, value : 횟수
         */
        Map<String,Integer> result = new HashMap<>();
        /*
            쿼리문을 하나씩 던지는 것 보다는 한번에 모아서 던지는게 더 효율적이라고 배워서 이를 위해 만든 list.
         */
        List<MarginForCampaign> marginForCampaigns = new ArrayList<>();
        /*
            이미 존재하는 MarginForCampaign에 대한 검증을 위한 Map.
            key : 캠패인 id , value : 해당 캠패인 옵션 이름.
         */
        Map<Long,Set<String>> optionNamesAboutCampaign = extractedCampaignIdAndOptionNames(email);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            result.put("total",sheet.getLastRowNum());
            result.put("error",0);
            result.put("update",0);
            ExcelDataLoop:
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                try {
                    MfcDto mfcDto = readExcelRowToMarginForCampaignData(row);
                    Long campaignID = Long.valueOf(getCellStringValue(row.getCell(0)));
                    if(optionNamesAboutCampaign.get(campaignID).contains(mfcDto.getMfcProductName())){
                        executeUpdateMarginForCampaignEntity(mfcDto,campaignID);
                        result.put("update", result.get("update") + 1);
                        continue;
                    }
                    Campaign campaign = campaignService.getMyCampaign(campaignID,email);
                    marginForCampaigns.add(
                            TypeChangeMarginForCampaign.createDtoToMargin(mfcDto,campaign)
                    );
                }catch (GrouException e) {
                    // 액셀의 row를 읽다가 생기는 예외
                    if (e.getErrorCode().equals(ErrorCode.FILE_INVALID_DATA_FORM)) {
                        // 액셀 업로드 작업을 계속 진행, 잘못 입력된 row의 정보를 사용자에게 전달한다.
                        result.put("error", result.get("error") + 1);
                        continue ExcelDataLoop;
                    }
                    // 해당 캠패인을 찾지 못할때 생기는 예외
                    if (e.getErrorCode().equals(ErrorCode.CAMPAIGN_NOT_FOUND)) {
                        // 액셀 업로드 작업을 중지, 찾지 못한 캠패인 아이디를 사용자에게 명시하여 수정하도록 유도한다.
                        log.error("MFC 액셀 업로드 과정 중 에러 발생 : {}", e.getErrorCode().getMessage());
                        throw e;
                    }
                    // 수정을 위해 조회 시 해당 옵션 이름을 찾지 못할 때
                    if(e.getErrorCode().equals(ErrorCode.MARGIN_FOR_CAMPAIGN_PRODUCT_NAME_NOT_FOUND)){
                        // 액셀 업로드 작업을 중지, 치명적 에러로 그대로 상위 레이어로 throw
                        log.error("MFC 액셀 업로드 과정 중 에러 발생 : {}", e.getErrorCode().getMessage());
                        throw  e;
                    }
                    throw e;
                }
            }
            marginForCampaignRepository.saveAll(marginForCampaigns);
            result.put("input",marginForCampaigns.size());
            return result;
        }
    }

    @Transactional
    public void executeUpdateMarginForCampaignEntity(MfcDto mfcDto,Long campaignId){
        MarginForCampaign oldMarginForCampaign = marginForCampaignRepository.findByCampaignAndMfcProductName(
                mfcDto.getMfcProductName(),
                campaignId).orElseThrow(
                ()-> new GrouException(ErrorCode.MARGIN_FOR_CAMPAIGN_PRODUCT_NAME_NOT_FOUND)
        );
        oldMarginForCampaign.updateExistingProduct(mfcDto);
    }

    private Map<Long,Set<String>> extractedCampaignIdAndOptionNames(String email) {
        Map<Long,Set<String>> optionNamesAboutCampaign = new HashMap<>();
        List<MarginForCampaignOptionNameAndCampaignId> marginForCampaignOptionNameAndCampaignIds =
                marginForCampaignRepository.findByCampaignEmail(email);
        for(MarginForCampaignOptionNameAndCampaignId dto : marginForCampaignOptionNameAndCampaignIds){
            optionNamesAboutCampaign.computeIfAbsent(dto.campaignId(), k -> new HashSet<>()).add(dto.optionName());
        }
        return optionNamesAboutCampaign;
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
