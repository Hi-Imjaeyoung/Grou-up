package growup.spring.springserver.memo.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.exception.memo.MemoNotFoundException;
import growup.spring.springserver.memo.dto.MemoRequestDto;
import growup.spring.springserver.memo.MemoTypeChanger;
import growup.spring.springserver.memo.dto.MemoUpdateRequestDto;
import growup.spring.springserver.memo.domain.Memo;
import growup.spring.springserver.memo.repository.MemoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MemoService {
    @Autowired
    private MemoRepository memoRepository;

    public Memo makeMemo(Campaign campaign, MemoRequestDto memoRequestDto){
        Memo memo = MemoTypeChanger.requestToEntity(memoRequestDto,campaign);
        return memoRepository.save(memo);
    }

    public List<Memo> getMemoAboutCampaign(Campaign campaign){
        return memoRepository.findAllByCampaign(campaign);
    }

    @Transactional
    public Memo updateMemo(MemoUpdateRequestDto memoRequestDto){
        Memo memo = memoRepository.findById(memoRequestDto.getMemoId()).orElseThrow(MemoNotFoundException::new
        );
        memo.updateContents(memoRequestDto.getContents());
        return memo;
    }

    @Transactional
    public int deleteMemo(Long id){
        return memoRepository.deleteMemoById(id);
    }

    @Transactional
    public int deleteKeywordByCampaignIdsAndDate(List<Long> campaignIds, LocalDate start , LocalDate end){
        return memoRepository.deleteByCampaignIdAndDate(start,end,campaignIds);
    }

    public Map<String,List<String>> getMemoByDateAndCampaign(LocalDate start, LocalDate end, Long campaignId){
        List<Memo> result = memoRepository.findByDateAndCampaignId(start,end,campaignId);
        Map<String,List<String>> map = new HashMap<>();
        for(Memo memo : result){
            if(!map.containsKey(memo.getRawDate())){
                map.put(memo.getRawDate(), new ArrayList<>());
            }
            map.get(memo.getRawDate()).add(memo.getContents());
        }
        return map;
    }

}
