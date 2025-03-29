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
import java.util.List;

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

    public int deleteKeywordByCampaignIdsAndDate(List<Long> campaignIds, LocalDate start , LocalDate end){
        return memoRepository.deleteByCampaignIdAndDate(start,end,campaignIds);
    }

}
