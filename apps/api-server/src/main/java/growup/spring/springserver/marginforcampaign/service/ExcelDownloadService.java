package growup.spring.springserver.marginforcampaign.service;

import growup.spring.springserver.campaign.dto.CampaignIdAndNameForExcelDownload;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.global.common.ExcelUtil;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor

public class ExcelDownloadService{
    private final MarginForCampaignRepository marginForCampaignRepository;
    private final CampaignService campaignService;

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
}
