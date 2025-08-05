package growup.spring.springserver.keyword.service;

import growup.spring.springserver.keyword.domain.Keyword;
import growup.spring.springserver.keyword.repository.KeywordRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
public class KeywordQueryService {
    private final KeywordRepository keywordRepository;

    public List<Keyword> findAllByDateANDCampaign(LocalDate start,
                                                  LocalDate end,
                                                  Long campaignId){
        return keywordRepository.findAllByDateANDCampaign(start,end,campaignId);
    }

}
