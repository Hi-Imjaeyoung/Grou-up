package growup.spring.springserver.keywordBid.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.keywordBid.KeywordBidNotFound;
import growup.spring.springserver.keywordBid.TypeChangeKeywordBid;
import growup.spring.springserver.keywordBid.domain.KeywordBid;
import growup.spring.springserver.keywordBid.dto.KeywordBidDto;
import growup.spring.springserver.keywordBid.dto.KeywordBidRequestDtos;
import growup.spring.springserver.keywordBid.dto.KeywordBidResponseDto;
import growup.spring.springserver.keywordBid.repository.KeywordBidRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KeywordBidService {
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private KeywordBidRepository keywordBidRepository;

    //여기서 CampaignService를 호출하는 것 보다, Controller 단에서 호출된 결과를 파라미터로 넣어주는건 어떨까
    public KeywordBidResponseDto addKeywordBids(KeywordBidRequestDtos requests,Campaign campaign){
        int requestDataSize = requests.getData().size();
        List<KeywordBid> list = checkOverlapKeywordBid(requests.getData(),campaign);
        int saveDataSize = list.size();
        keywordBidRepository.saveAll(list);
        return KeywordBidResponseDto.builder()
                .requestNumber(requestDataSize)
                .responseNumber(saveDataSize)
                .build();
    }

    public List<KeywordBid> checkOverlapKeywordBid(List<KeywordBidDto> keywordBidDtos, Campaign campaign){
        List<KeywordBid> list = new ArrayList<>();
        for(KeywordBidDto dto : keywordBidDtos){
            if(keywordBidRepository.existsByCampaign_CampaignIdAndKeyword(campaign.getCampaignId(),dto.getKeyword())) continue;
            list.add(KeywordBid.builder()
                    .keyword(dto.getKeyword())
                    .campaign(campaign)
                    .bid(dto.getBid())
                    .build()
            );
        }
        return list;
    }

    public KeywordBidResponseDto getKeywordBids(Long campaignId){
        List<KeywordBid> list =  keywordBidRepository.findAllByCampaign_CampaignId(campaignId);
        KeywordBidResponseDto keywordBidResponseDto = new KeywordBidResponseDto();
        keywordBidResponseDto.setResponse(list.stream().map(TypeChangeKeywordBid::entityToDto).toList());
        return keywordBidResponseDto;
    }

    @Transactional
    public KeywordBidResponseDto updateKeywordBids(KeywordBidRequestDtos request){
        int requestNumber = request.getData().size();
        int updateNumber = requestNumber;
        List<KeywordBidDto> list = new ArrayList<>();
        for(KeywordBidDto data :request.getData()){
            try {
                KeywordBid nowData = findKeywordBidByCampaignIDAndKey(request.getCampaignId(), data.getKeyword());
                nowData.updateBid(data.getBid());
                list.add(TypeChangeKeywordBid.entityToDto(nowData));
            }catch (KeywordBidNotFound error){
                // 여러개의 update 값 중 하나를 찾지 못한 경우 로직이 계속 진행되도록 하고 싶었습니다.
                updateNumber --;
            }
        }
        // update 된 데이터의 수를 보내서 화면에 보여줄 계획입니다.
        return KeywordBidResponseDto.builder()
                .requestNumber(requestNumber)
                .response(list)
                .responseNumber(updateNumber)
                .build();
    }

    public KeywordBid findKeywordBidByCampaignIDAndKey(Long campaignId, String key){
        return keywordBidRepository.findByCampaign_CampaignIdANDKeyword(campaignId,key).orElseThrow(
                KeywordBidNotFound::new
        );
    }

    @Transactional
    public KeywordBidResponseDto deleteByCampaignIdAndKeyword(KeywordBidRequestDtos request){
        int requestNumber = request.getData().size();
        int deleteNumber = 0;
        for(KeywordBidDto data : request.getData()){
            deleteNumber += keywordBidRepository.deleteByCampaign_CampaignIdANDKeyword(request.getCampaignId(),data.getKeyword());
        }
        return KeywordBidResponseDto.builder()
                .requestNumber(requestNumber)
                .responseNumber(deleteNumber)
                .build();
    }
    public int deleteKeywordBidsByCampaignIds(List<Long> campaignIds){
        return keywordBidRepository.deleteAllByCampaignIds(campaignIds);
    }
}
