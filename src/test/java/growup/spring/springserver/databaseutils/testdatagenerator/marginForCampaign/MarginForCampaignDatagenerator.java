package growup.spring.springserver.databaseutils.testdatagenerator.marginForCampaign;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.databaseutils.testdatagenerator.DataFormat;
import growup.spring.springserver.databaseutils.testdatagenerator.RandomOptionNameGenerator;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("bottleNeckTest")
public class MarginForCampaignDatagenerator {
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private MarginForCampaignRepository marginForCampaignRepository;

    @ParameterizedTest
    @CsvSource({
            "1, 500"
    })
    void makeMFCData(String startMemberId, String endMemberId) throws ChangeSetPersister.NotFoundException {
        for(int i=Integer.parseInt(startMemberId);i<Integer.parseInt(endMemberId);i++){
            String counter = String.valueOf(i);
            if(i<10) {
                counter = "0" + i;
            }
            String email = DataFormat.USERNAME.getValue() +counter+ DataFormat.EMAIL_FORMAT.getValue();
            Member member = memberRepository.findByEmail(email).orElseThrow(
                    ChangeSetPersister.NotFoundException::new
            );
            List<Campaign> campaignList = campaignRepository.findAllByMember(member);
            for(Campaign campaign : campaignList){
                for(int j=0;j<5;j++){
                    MarginForCampaign.builder()
                            .mfcProductName(RandomOptionNameGenerator.getRandomOptionNames())
                            .campaign(campaign)
                            .build();
                    marginForCampaignRepository.save(
                            MarginForCampaign.builder()
                                    .mfcProductName(RandomOptionNameGenerator.getRandomOptionNames())
                                    .mfcType(MarginType.ROCKET_GROWTH)
                                    .mfcTotalPrice(1L)
                                    .mfcSalePrice(1L)
                                    .mfcReturnPrice(1L)
                                    .mfcCostPrice(1L)
                                    .campaign(campaign)
                                    .build()
                    );
                }
            }
        }
    }
}
