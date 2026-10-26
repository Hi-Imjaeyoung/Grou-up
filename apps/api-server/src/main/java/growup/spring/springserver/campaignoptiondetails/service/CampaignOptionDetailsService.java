package growup.spring.springserver.campaignoptiondetails.service;

import growup.spring.springserver.campaignoptiondetails.TypeChangeCampaignOptionDetails;
import growup.spring.springserver.campaignoptiondetails.domain.CampaignOptionDetails;
import growup.spring.springserver.campaignoptiondetails.dto.CampaignOptionDetailsResponseDto;
import growup.spring.springserver.campaignoptiondetails.repository.CampaignOptionDetailsRepository;
import growup.spring.springserver.execution.service.ExecutionService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@AllArgsConstructor
@Slf4j
public class CampaignOptionDetailsService {
    private final CampaignOptionDetailsRepository campaignOptionDetailsRepository;
    private final ExecutionService executionService;

    public List<CampaignOptionDetails> getRawCampaignDetails(LocalDate start, LocalDate end, long id) {
        List<Long> byCampaignCampaignIds = executionService.findExecutionIdsByCampaignId(id);
        if (byCampaignCampaignIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<CampaignOptionDetails> byExecutionIdsAndDateRange = campaignOptionDetailsRepository.findByExecutionIdsAndDateRange(
                byCampaignCampaignIds, start, end);
        if (byExecutionIdsAndDateRange.isEmpty()) {
            return Collections.emptyList();
        }
        return byExecutionIdsAndDateRange;
    }

    public List<CampaignOptionDetailsResponseDto> getCampaignDetailsByCampaignsIds(LocalDate start, LocalDate end, long id) {
        List<CampaignOptionDetails> data = getRawCampaignDetails(start, end, id);

        HashMap<Long, CampaignOptionDetailsResponseDto> map = new HashMap<>();
        for(CampaignOptionDetails details : data){

            if(map.containsKey(details.getExecution().getExeId())){
                map.get(details.getExecution().getExeId()).update(details);
                continue;
            }
            map.put(details.getExecution().getExeId(), TypeChangeCampaignOptionDetails.entityToResponseDto(details));
        }

        List<CampaignOptionDetailsResponseDto> campaignOptionDetailsResponseDtos = new ArrayList<>();
        for(Long key : map.keySet()){
            campaignOptionDetailsResponseDtos.add(map.get(key));
        }
        return campaignOptionDetailsResponseDtos;
    }

    @Transactional
    public int deleteKeywordByExecutionIdsAndDate(List<Long> executionIds, LocalDate start, LocalDate end){
        return campaignOptionDetailsRepository.deleteByCampaignIdAndDate(start,end,executionIds);
    }

    public int deleteCampaignOptionDetailsByExecutionIds(List<Long> campaignIds){
        return campaignOptionDetailsRepository.deleteAllByCampaignIds(campaignIds);
    }
}