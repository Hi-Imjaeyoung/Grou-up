package growup.spring.springserver.marginforcampaign.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.marginforcampaign.MarginForCampaignFoundException;
import growup.spring.springserver.exception.marginforcampaign.MarginForCampaignIdNotFoundException;
import growup.spring.springserver.exception.marginforcampaign.MarginForCampaignProductNameNotFoundException;
import growup.spring.springserver.marginforcampaign.TypeChangeMarginForCampaign;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.dto.MarginForCampaignResDto;
import growup.spring.springserver.marginforcampaign.dto.MfcDto;
import growup.spring.springserver.marginforcampaign.dto.MfcRequestDtos;
import growup.spring.springserver.marginforcampaign.dto.MfcValidationResponseDto;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
@Slf4j
public class MarginForCampaignService {
    private final MarginForCampaignRepository marginForCampaignRepository;
    private final CampaignService campaignService;

    public List<MarginForCampaignResDto> marginForCampaignByCampaignId(Long id) {
        List<MarginForCampaign> marginForCampaigns = marginForCampaignRepository.MarginForCampaignByCampaignId(id);

        ArrayList<MarginForCampaignResDto> datas = new ArrayList<>();
        for (MarginForCampaign data : marginForCampaigns) {
            datas.add(
                    TypeChangeMarginForCampaign.EntityToDto(data)
            );
        }
        return datas;
    }

    public MfcValidationResponseDto searchMarginForCampaignProductName(String email, MfcRequestDtos requestDtos) {
        Campaign campaign = campaignService.getMyCampaign(requestDtos.getCampaignId(),email);

        return validateProducts(requestDtos.getData(), email, campaign);
    }

    public void deleteMarginForCampaign(Long id) {
        if (!marginForCampaignRepository.existsById(id)) {
            throw new MarginForCampaignIdNotFoundException();
        }
        // 존재할 경우 삭제
        marginForCampaignRepository.deleteById(id);
    }

    private MfcValidationResponseDto validateProducts(List<MfcDto> datas, String email, Campaign campaign) {
        List<MarginForCampaign> successfulProducts = new ArrayList<>();
        List<String> failedProductNames = new ArrayList<>();
        Map<String, Set<MarginType>> productNamesMap = new HashMap<>(); // 중복 체크를 위한 Map

        handleProductValidation(datas, email, campaign, productNamesMap, failedProductNames, successfulProducts);

        saveSuccessfulProducts(successfulProducts);

        return createValidationResponse(datas.size(), successfulProducts.size(), failedProductNames);
    }

    private void handleProductValidation(List<MfcDto> datas, String email, Campaign campaign,
                                         Map<String, Set<MarginType>> productNamesMap,
                                         List<String> failedProductNames,
                                         List<MarginForCampaign> successfulProducts) {
        for (MfcDto data : datas) {
            if (isProductAlreadyProcessed(data, productNamesMap)) {
                failedProductNames.add(data.getMfcProductName());
                continue;
            }

            try {
                // ✅ 현재 캠페인 내에서만 중복을 체크합니다.
                validateUniqueProductInMyCampaign(data, campaign);

                // ✅ 중복 검사 로직이 하나 줄었으므로, 여기서 바로 handleProduct를 호출해야 합니다.
                handleProduct(data, campaign, successfulProducts);

            } catch (MarginForCampaignFoundException exception) {
                handleExistingProduct(data, campaign, successfulProducts, failedProductNames);
            }
        }
    }
    /*
    * TODO
    *  computeIfAbsent :
    *   상품명과 타입 기준으로 set가져옴, 없으면 생성
    * */
    private boolean isProductAlreadyProcessed(MfcDto data, Map<String, Set<MarginType>> productNamesMap) {
        String productName = data.getMfcProductName();
        MarginType productType = data.getMfcType();
//        상품명 + 타입의 조합
        Set<MarginType> typesSet = productNamesMap.computeIfAbsent(productName, k -> new HashSet<>());
        if (typesSet.contains(productType)) {
            return true; // 이미 처리한 상품
        }

        typesSet.add(productType); // 타입을 추가
        return false; // 처리 안 한 상품
    }
    private void handleExistingProduct(MfcDto data, Campaign campaign,
                                       List<MarginForCampaign> successfulProducts,
                                       List<String> failedProductNames) {
        log.info(data.getMfcProductName() + " is already processed in this campaign.");
        if (data.getMfcId() == null) {
            failedProductNames.add(data.getMfcProductName());
        } else {
            handleProduct(data, campaign, successfulProducts);
        }
    }

    // 자기 캠페인에서 중복 체크
    private void validateUniqueProductInMyCampaign(MfcDto data, Campaign campaign) {
        if (marginForCampaignRepository.existsByCampaignAndMfcProductNameAndMfcType(campaign, data.getMfcProductName(), data.getMfcType())) {
            throw new MarginForCampaignFoundException();
        }
    }

    private void handleProduct(MfcDto data, Campaign campaign, List<MarginForCampaign> successfulProducts) {
//         상품명으로 찾을경우 상품명을 수정하였을때 새롭게 같은이름으로 db가 들어감
//        Optional<MarginForCampaign> existingProduct = marginForCampaignRepository.findByCampaignAndMfcProductName(data.getMfcProductName(), campaign.getCampaignId());

//        캠페인 명과 MfcId 로 찾음
        Optional<MarginForCampaign> existingProduct = marginForCampaignRepository.findByCampaignAndMfcId(data.getMfcId(), campaign.getCampaignId());
//        있는 경우는 있는거 return 후 add
        MarginForCampaign product = existingProduct.map(existing -> {
            existing.updateExistingProduct(data);
            return existing;
//            없으면 새로운거 생성
        }).orElseGet(() -> createMarginFromDto(data, campaign));
//        있으면 Existing 없으면 createMarginFromDto 로 생성된것 add 해줌
        successfulProducts.add(product);
    }


    private MarginForCampaign createMarginFromDto(MfcDto data, Campaign campaign) {
        return TypeChangeMarginForCampaign.createDtoToMargin(data, campaign);
    }

    private void saveSuccessfulProducts(List<MarginForCampaign> successfulProducts) {
        marginForCampaignRepository.saveAll(successfulProducts);

    }

    private MfcValidationResponseDto createValidationResponse(int requestCount, int successCount, List<String> failedProductNames) {
        return TypeChangeMarginForCampaign.validationResponse(requestCount, successCount, failedProductNames);
    }
    /*
    ** TODO
    *   email, camapign,상품명, type(판매자배송, 로켓그로스)
    *   현재 캠페인을 제외하고 중복되는 이름이 있는지 확인
     */
//    private void validateUniqueProductNameInOtherCampaigns(String email, MfcDto data, Campaign campaign) {
//        marginForCampaignRepository.findByEmailAndMfcProductNameExcludingCampaign(email, data.getMfcProductName(), campaign.getCampaignId(), data.getMfcType())
//                .orElseThrow(MarginForCampaignProductNameNotFoundException::new);
//    }

    public List<MarginForCampaignResDto>  getMyAllExecution(String username) {
        List<MarginForCampaign> allByMemberEmailWithFetch = marginForCampaignRepository.findAllByMemberEmailWithFetch(username);

        return allByMemberEmailWithFetch.stream()
                .map(TypeChangeMarginForCampaign::EntityToDtoWithCampaign) // 각 data를 DTO로 변환
                .toList();
    }
    public MarginForCampaign getMyMarginForCampaignById(Long id) {
        return marginForCampaignRepository.findById(id)
                .orElseThrow(MarginForCampaignIdNotFoundException::new);
    }

    public int deleteMFC(List<Long> campaignIds){
        return marginForCampaignRepository.deleteAllByCampaignIds(campaignIds);
    }
}