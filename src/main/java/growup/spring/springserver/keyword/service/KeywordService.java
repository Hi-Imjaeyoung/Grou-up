package growup.spring.springserver.keyword.service;

import growup.spring.springserver.exception.InvalidDateFormatException;
import growup.spring.springserver.exception.exclusionKeyword.ExclusionKeyNotFound;
import growup.spring.springserver.exception.keyword.CampaignKeywordNotFoundException;
import growup.spring.springserver.exclusionKeyword.dto.ExclusionKeywordResponseDto;
import growup.spring.springserver.exclusionKeyword.service.ExclusionKeywordService;
import growup.spring.springserver.keyword.domain.Keyword;
import growup.spring.springserver.keyword.dto.KeywordResponseDto;
import growup.spring.springserver.keyword.TypeChangeKeyword;
import growup.spring.springserver.keyword.dto.KeywordTotalDataResDto;
import growup.spring.springserver.keyword.repository.KeywordRepository;
import growup.spring.springserver.keywordBid.dto.KeywordBidDto;
import growup.spring.springserver.keywordBid.dto.KeywordBidResponseDto;
import growup.spring.springserver.keywordBid.service.KeywordBidService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

@Service
public class KeywordService {
    @Autowired
    private KeywordRepository keywordRepository;
    @Autowired
    private ExclusionKeywordService exclusionKeywordService;
    @Autowired
    private KeywordBidService keywordBidService;
    public List<KeywordResponseDto> getKeywordsByCampaignId(LocalDate start, LocalDate end , Long campaignId){
        if(!checkDateFormat(start,end)) throw new InvalidDateFormatException();
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

    public boolean checkDateFormat(LocalDate start , LocalDate end){
        try{
            if(start.isAfter(end)) throw new DataFormatException();
        }catch (DateTimeParseException | DataFormatException e){
           return false;
        }
        return true;
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
            map.get(dto.getKeyword()).setBid(dto.getBid());
            keywordResponseDtos.add(map.get(dto.getKeyword()));
        }
        return keywordResponseDtos;
    }

    public List<KeywordResponseDto> getKeywordsByDateAndCampaignIdAndKeys(LocalDate start,
                                                                          LocalDate end,
                                                                          Long campaignId,
                                                                          List<KeywordBidDto> keys){
        if(!checkDateFormat(start,end)) throw new InvalidDateFormatException();
        List<Keyword> data =
                keywordRepository.findKeywordsByDateAndCampaignIdAndKeys(start,end,campaignId,keys.stream().map(KeywordBidDto::getKeyword).toList());
        return addBids(summeryKeywordData(data),keys);
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

    public KeywordTotalDataResDto getTotalData(LocalDate start,
                                               LocalDate end,
                                               Long campaignId){
        List<Keyword> data = keywordRepository.findAllByDateANDCampaign(start,end,campaignId);
        Map<LocalDate,KeywordResponseDto> search = new HashMap<>();
        Map<LocalDate,KeywordResponseDto> nonSearch = new HashMap<>();
        for(Keyword keyword : data){
            //nonSearch
            if(keyword.getKeyKeyword().equals("-")){
                if(nonSearch.containsKey(keyword.getKeyDate())){
                    nonSearch.get(keyword.getKeyDate()).update(keyword);
                    continue;
                }
                nonSearch.put(keyword.getKeyDate(),TypeChangeKeyword.entityToResponseDto(keyword));
                continue;
            }
            //search
            if(search.containsKey(keyword.getKeyDate())){
                search.get(keyword.getKeyDate()).update(keyword);
                continue;
            }
            search.put(keyword.getKeyDate(),TypeChangeKeyword.entityToResponseDto(keyword));
        }
        return KeywordTotalDataResDto.builder()
                .search(search)
                .nonSearch(nonSearch)
                .build();
    }
    @Transactional
    public int deleteKeywordByCampaignIdsAndDate(List<Long> campaignIds,LocalDate start, LocalDate end){
        return keywordRepository.deleteByCampaignIdAndDate(start,end,campaignIds);
    }
}
