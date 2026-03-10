package growup.spring.springserver.campaign.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import growup.spring.springserver.login.domain.Member;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
@Transactional(readOnly = true)
public class CampaignService {
    private final CampaignRepository campaignRepository;

    /**
     * 이메일을 기반으로 회원이 속한 캠페인 목록 조회
     */
    public List<Campaign> getCampaignsByEmail(String email) {
        List<Campaign> campaignList = campaignRepository.findAllByEmail(email);

        if (campaignList.isEmpty()) throw new CampaignNotFoundException();

        return campaignList;
    }
    public List<Campaign> getCampaignsByEmailPossibleEmpty(String email) {

        return campaignRepository.findAllByEmail(email);
    }

    public List<Campaign> getCampaignsByMember(Member member) {
        List<Campaign> campaignList = campaignRepository.findAllByMember(member);

        if (campaignList.isEmpty()) throw new CampaignNotFoundException();

        return campaignList;
    }
    /**
     * 특정 캠페인 단건 조회 (이메일 + 캠페인 ID 기반)
     */
    @Cacheable(value = "campaigns", key = "#campaignId + '_' + #email")
    public Campaign getMyCampaign(Long campaignId, String email) {
        return campaignRepository.findByCampaignIdANDEmail(campaignId, email)
                .orElseThrow(CampaignNotFoundException::new);
    }
    @Transactional
    @CacheEvict(value = "campaigns", allEntries = true)
    public int deleteCampaign(List<Long> campaignIds){
            try {
                return campaignRepository.deleteAllByCampaignIds(campaignIds);
            }catch (ConstraintViolationException exception){
                log.error("FK 제약조건 설정이 위배되어 캠패인이 삭제 되지 않았습니다.");
                throw new GrouException(ErrorCode.FK_CONSTRAINT);
            }
    }
}
