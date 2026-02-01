package growup.spring.springserver.keyword.service;

import com.querydsl.core.Tuple;
import growup.spring.springserver.campaign.dto.CampaignAnalysisDto;
import growup.spring.springserver.exception.global.InvalidDateFormatException;
import growup.spring.springserver.exception.exclusionKeyword.ExclusionKeyNotFound;
import growup.spring.springserver.exception.global.RequestException;
import growup.spring.springserver.exception.keyword.CampaignKeywordNotFoundException;
import growup.spring.springserver.exclusionKeyword.dto.ExclusionKeywordResponseDto;
import growup.spring.springserver.exclusionKeyword.service.ExclusionKeywordService;
import growup.spring.springserver.global.cache.SegTreeCacheManager;
import growup.spring.springserver.keyword.domain.Keyword;
import growup.spring.springserver.keyword.dto.KeywordResponseDto;
import growup.spring.springserver.keyword.TypeChangeKeyword;
import growup.spring.springserver.keyword.dto.KeywordTotalDataResDto;
import growup.spring.springserver.keyword.repository.KeywordRepository;
import growup.spring.springserver.keywordBid.dto.KeywordBidDto;
import growup.spring.springserver.keywordBid.dto.KeywordBidResponseDto;
import growup.spring.springserver.keywordBid.service.KeywordBidService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

import static growup.spring.springserver.campaign.domain.QCampaign.campaign;
import static growup.spring.springserver.keyword.domain.QKeyword.keyword;

@Service
@AllArgsConstructor
@Slf4j
public class KeywordService {
    private final KeywordRepository keywordRepository;
    private final ExclusionKeywordService exclusionKeywordService;
    private final KeywordBidService keywordBidService;
    private final SegTreeCacheManager segTreeCacheManager;

    public List<KeywordResponseDto> getKeywordsByCampaignId(LocalDate start, LocalDate end , Long campaignId){
        List<Keyword> data = keywordRepository.findAllByDateANDCampaign(start,end,campaignId);
        List<KeywordResponseDto> result = checkKeyTypeExclusion(summeryKeywordData(data),getExclusionKeywordToSet(campaignId));
        checkKeyTypeKeywordBid(result,getBidKeywrodToSet(campaignId));
        return result;
    }

    public Set<String> getExclusionKeywordToSet(Long campaignId){
        List<ExclusionKeywordResponseDto> exclusionKeywordResponseDtos;
        try {
            exclusionKeywordResponseDtos = exclusionKeywordService.getExclusionKeywords(campaignId);
        }catch (ExclusionKeyNotFound e){
            exclusionKeywordResponseDtos = List.of();
        }
        return exclusionKeywordResponseDtos.stream()
                .map(ExclusionKeywordResponseDto::getExclusionKeyword)
                .collect(Collectors.toSet());
    }

    public HashMap<String,KeywordResponseDto> summeryKeywordData(List<Keyword> data){
        if(data.isEmpty()) throw new CampaignKeywordNotFoundException();
        HashMap<String,KeywordResponseDto> map = new HashMap<>();
        for(Keyword keyword : data){
            if(keyword.getKeyKeyword() == null || keyword.getKeyKeyword().isEmpty()){
                continue;
            }
            if(map.containsKey(keyword.getKeyKeyword())){
                KeywordResponseDto dto = map.get(keyword.getKeyKeyword());
                summeryKeySalesOption(dto.getKeySalesOptions(),keyword.getKeyProductSales());
                map.get(keyword.getKeyKeyword()).update(keyword);
                continue;
            }
            map.put(keyword.getKeyKeyword(),TypeChangeKeyword.entityToResponseDto(keyword));
        }
        return map;
    }

    public void summeryKeySalesOption(Map<String,Long> originData, Map<String,Long> inputData){
        if(inputData == null) return;
        for(String inputKey : inputData.keySet()){
            originData.put(inputKey,originData.getOrDefault(inputKey,0L)+inputData.get(inputKey));
        }
    }
    public List<KeywordResponseDto> checkKeyTypeExclusion(HashMap<String,KeywordResponseDto> map, Set<String> exclusions){
            List<KeywordResponseDto> keywordResponseDtos = new ArrayList<>();
            for(String key : map.keySet()){
                if(!exclusions.isEmpty() &&exclusions.contains(key)) map.get(key).setKeyExcludeFlag(true);
                keywordResponseDtos.add(map.get(key));
            }
            return keywordResponseDtos;
        }

    public List<KeywordResponseDto> addBids(HashMap<String,KeywordResponseDto> map,List<KeywordBidDto> keys){
        List<KeywordResponseDto> keywordResponseDtos = new ArrayList<>();
        for(KeywordBidDto dto : keys){
            // map에 없는 키워드를 찾아 null 발생
            if(map.containsKey(dto.getKeyword())){
                map.get(dto.getKeyword()).setBid(dto.getBid());
                keywordResponseDtos.add(map.get(dto.getKeyword()));
            }
        }
        return keywordResponseDtos;
    }

    public List<KeywordResponseDto> getKeywordsByDateAndCampaignIdAndKeys(LocalDate start,
                                                                          LocalDate end,
                                                                          Long campaignId,
                                                                          List<KeywordBidDto> keys){
        try {
            List<Keyword> data =
                    keywordRepository.findKeywordsByDateAndCampaignIdAndKeys(start,end,campaignId,keys.stream().map(KeywordBidDto::getKeyword).toList());
            return addBids(summeryKeywordData(data),keys);
        }catch (CampaignKeywordNotFoundException e){
            return List.of();
        }
    }

    public Set<String > getBidKeywrodToSet(Long campaignId){
        KeywordBidResponseDto keywordBidResponseDto = keywordBidService.getKeywordBids(campaignId);
        return keywordBidResponseDto.getResponse().stream().map(KeywordBidDto::getKeyword).collect(Collectors.toSet());
    }

    public void checkKeyTypeKeywordBid (List<KeywordResponseDto> data, Set<String> bidKey){;
        for(KeywordResponseDto key : data){
            if(!bidKey.isEmpty() && bidKey.contains(key.getKeyKeyword())) key.setKeyBidFlag(true);
        }
    }
    @Transactional
    public int deleteKeywordByCampaignIdsAndDate(List<Long> campaignIds,LocalDate start, LocalDate end){
        return keywordRepository.deleteByCampaignIdAndDate(start,end,campaignIds);
    }

    public int deleteKeywordByCampaignIds(List<Long> campaignIds){
        return keywordRepository.deleteAllByCampaignIds(campaignIds);
    }

    //
    public Map<String, CampaignAnalysisDto> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(String email,LocalDate start, LocalDate end){
        List<Tuple> queryResult = keywordRepository.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(start,end,email);
        Map<String, CampaignAnalysisDto> campaignTypeResultMap = new HashMap<>();
        double totalCost = 0.0;
        double totalSales = 0.0;
        for (Tuple tuple : queryResult) {
            LocalDate date = tuple.get(keyword.date);
            String type = tuple.get(campaign.camAdType);
            Double cost = tuple.get(keyword.adCost.sum());
            Double sales = tuple.get(keyword.adSales.sum());
            double safeCost = (cost != null) ? cost : 0.0;
            double safeSales = (sales != null) ? sales : 0.0;
            segTreeCacheManager.addLeafNode(email,date,type,safeCost,safeSales);
            campaignTypeResultMap.merge(type,
                    new CampaignAnalysisDto(safeCost, safeSales), // 1. 새로운 값 (초기값으로 쓰임)
                    (oldDto, newDto) -> oldDto.add(newDto.getAdCost(), newDto.getAdSales()) // 2. 합치는 로직 (람다)
            );
            totalCost += safeCost;
            totalSales += safeSales;
        }
        campaignTypeResultMap.put("총 매출", new CampaignAnalysisDto(totalCost, totalSales));
        return campaignTypeResultMap;
    }

    public List<Tuple> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByList(String email,LocalDate start, LocalDate end){
        return keywordRepository.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByCache(start,end,email);
    }

    public Map<String, CampaignAnalysisDto> getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmail(String email,LocalDate start, LocalDate end){
        List<Tuple> queryResult = keywordRepository.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmail(start,end,email);
        Map<String,CampaignAnalysisDto> map = new HashMap<>();
        Double totalAdCost = 0.0;
        Double totalAdSale = 0.0;
        for(Tuple tuple: queryResult){
            Double adCostSum = tuple.get(keyword.adCost.sum());
            Double adSaleSum = tuple.get(keyword.adSales.sum());
            totalAdSale += adSaleSum;
            totalAdCost += adCostSum;
            map.put(tuple.get(campaign.camAdType),new CampaignAnalysisDto(adCostSum,adSaleSum));
        }
        map.put("총 매출",new CampaignAnalysisDto(totalAdCost,totalAdSale));
        return map;
    }

    public Map<String, CampaignAnalysisDto> getEachCampaignAdCostSumAndAdSalesByPeriodAndEmail(String email,LocalDate start, LocalDate end){
        List<Tuple> queryResult = keywordRepository.getEachCampaignAdCostSumAndAdSalesByEmail(start,end,email);
        return queryResult.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(campaign.camCampaignName),
                        tuple -> {
                            Double adCostSum = tuple.get(keyword.adCost.sum());
                            Double adSaleSum = tuple.get(keyword.adSales.sum());
                            return new CampaignAnalysisDto(
                                    adCostSum != null ? adCostSum : 0,
                                    adSaleSum != null ? adSaleSum : 0
                            );
                        }
                ));
    }
}
