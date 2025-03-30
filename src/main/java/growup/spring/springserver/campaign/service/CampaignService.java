package growup.spring.springserver.campaign.service;

import growup.spring.springserver.campaign.TypeChangeCampaign;
import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.dto.CampaignResponseDto;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.global.exception.GrouException;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CampaignService {
    @Autowired
    private MemberService memberService;
    @Autowired
    private CampaignRepository campaignRepository;

    //TODO: 예외 클래스 변경 및 반환 데이터에 ID로 넣어야할까 고민중.
    /**
     * 이메일을 기반으로 회원의 모든 캠페인 조회 (DTO 변환)
     */
    public List<CampaignResponseDto> getMyCampaigns(String email) {
        return getCampaignsByEmail(email).stream()
                .map(TypeChangeCampaign::entityToResponseDto)
                .toList();
    }
    /**
     * 이메일을 기반으로 회원이 속한 캠페인 목록 조회
     */
    public List<Campaign> getCampaignsByEmail(String email) {
        Member member = memberService.getMemberByEmail(email);
        List<Campaign> campaignList = campaignRepository.findAllByMember(member);

        if (campaignList.isEmpty()) throw new CampaignNotFoundException();

        return campaignList;
    }
    /**
     * 특정 캠페인 단건 조회 (이메일 + 캠페인 ID 기반)
     */
    public Campaign getMyCampaign(Long campaignId, String email) {
        return campaignRepository.findByCampaignIdANDEmail(campaignId, email)
                .orElseThrow(CampaignNotFoundException::new);
    }

    public int deleteCampaign(List<Long> campaignIds){
        int deletedCampaignNumber = 0;
        for(Long campaignId : campaignIds){
            try {
                if (campaignRepository.findByCampaignId(campaignId).isPresent()) {
                    campaignRepository.deleteById(campaignId);
                    deletedCampaignNumber++;
                }
            }catch (ConstraintViolationException exception){
                log.error("FK 제약조건 설정이 위배되어 "+campaignId+" 캠패인이 삭제 되지 않았습니다.");
            }
        }
        return deletedCampaignNumber;
    }

}
