package growup.spring.springserver.marginforcampaign.dto;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import growup.spring.springserver.marginforcampaign.TypeChangeMarginForCampaign;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ExcelUploadContext {
    /*
     업로드 결과를 저장하는 Map
     Key : insert(새로 추가된 옵션), update(기존 옵션), error(파일 형식 오류), total(전체 옵션)
     */
    private final Map<String,Integer> result = new HashMap<>();
    /*
     저장할 MFC 모아두는 List
     */
    private final List<MarginForCampaign> entitiesToInsert = new ArrayList<>();
    private final List<MarginForCampaign> entitiesToUpdate = new ArrayList<>();
    /*
     사용자가 올린 엑셀의 옵션명들을 value, campaignId를 key 로 갖는 Map
     */
    private final Map<Long,List<String>> optionNamesFromExcel = new HashMap<>();

    private final String email;
    private final Map<Long, Campaign> campaignCache;
    private final Map<Long,Map<String,MarginForCampaign>> myMFCCache;

    public ExcelUploadContext(int totalRow,
                              String email,
                              Map<Long,Campaign> campaignCache,
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
        this.entitiesToInsert.add(entity);
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
    public void upsertMfcDtoToEntitiesToSave(MarginForCampaign oldMFc, MfcDto mfcDto, Campaign campaign){
        if(oldMFc == null){
            entitiesToInsert.add(TypeChangeMarginForCampaign.createDtoToMargin(mfcDto, campaign));
            increment("input");
        }else{
            oldMFc.updateExistingProduct(mfcDto);
            entitiesToUpdate.add(oldMFc); // 영속성 컨텍스트에 있다면 사실 더티 체킹으로 끝나지만 명시적으로 분리
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

    public Campaign getRandomCampaign(){
        for(Long id : campaignCache.keySet()){
            return campaignCache.get(id);
        }
        return null;
    }

}
