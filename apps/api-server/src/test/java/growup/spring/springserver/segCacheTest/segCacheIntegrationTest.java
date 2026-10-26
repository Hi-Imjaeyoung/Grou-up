package growup.spring.springserver.segCacheTest;

import growup.spring.springserver.campaign.dto.CampaignAnalysisDto;
import growup.spring.springserver.campaign.facade.CampaignTotalDataFacade;
import growup.spring.springserver.global.cache.AllCampaignTypeData;
import growup.spring.springserver.global.cache.SegTreeCacheManager;
import growup.spring.springserver.keyword.repository.KeywordRepository;
import growup.spring.springserver.keyword.service.KeywordService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@ActiveProfiles(value = "bottleNeckTest")
public class segCacheIntegrationTest {
    @Autowired
    private KeywordService keywordService;
    @Autowired
    private KeywordRepository keywordRepository;
    @Autowired
    private SegTreeCacheManager segTreeCacheManager;
    @Autowired
    private CampaignTotalDataFacade campaignTotalDataFacade;

    private Map<String, String> memberInfo;

    @BeforeEach
    void setMemberData() {
        memberInfo = new HashMap<>();
        memberInfo.put("email", "user482@test.com");
        memberInfo.put("start", "2025-07-01");
        memberInfo.put("end", "2025-12-30");
    }

    @Test
    @DisplayName("세그먼트 트리 구조 생성 확인")
    void checkTreeMake() {
        String email = memberInfo.get("email");
        LocalDate start = LocalDate.parse(memberInfo.get("start"), DateTimeFormatter.ISO_DATE);
        LocalDate end = LocalDate.parse(memberInfo.get("end"), DateTimeFormatter.ISO_DATE);
        campaignTotalDataFacade.getCampaignTotalData(email,start,end);
        System.out.println(segTreeCacheManager.getCacheTree());

    }

    @Test
    @DisplayName("세그먼트 트리 RootKey 확인")
    void checkRootKey(){
        String email = memberInfo.get("email");
        LocalDate start = LocalDate.parse(memberInfo.get("start"), DateTimeFormatter.ISO_DATE);
        LocalDate end = LocalDate.parse(memberInfo.get("end"), DateTimeFormatter.ISO_DATE);
        campaignTotalDataFacade.getCampaignTotalData(email,start,end);
        System.out.println(segTreeCacheManager.getRootNodeKey(email));
    }

    @Test
    @DisplayName("세그먼트 트리 조회 검증 : 터미널창")
    void selectQueryPrintTerminal(){
        String email = memberInfo.get("email");
        LocalDate start = LocalDate.parse(memberInfo.get("start"), DateTimeFormatter.ISO_DATE);
        LocalDate end = LocalDate.parse(memberInfo.get("end"), DateTimeFormatter.ISO_DATE);
        campaignTotalDataFacade.getCampaignTotalData(email,start,end);
        LocalDate start2 = LocalDate.parse("2025-07-01",DateTimeFormatter.ISO_DATE);
        LocalDate end2 = LocalDate.parse("2025-08-30",DateTimeFormatter.ISO_DATE);
        AllCampaignTypeData data = segTreeCacheManager.checkPeriod(email,start2,end2);
        System.out.println(data.getCampaignAnalysisDtoMap().toString());
    }

    @Test
    @DisplayName("세그먼트 트리 조회 검증 : 값 확인")
    void selectQueryCompareValue(){
        // make Tree
        String email = memberInfo.get("email");
        LocalDate start = LocalDate.parse(memberInfo.get("start"), DateTimeFormatter.ISO_DATE);
        LocalDate end = LocalDate.parse(memberInfo.get("end"), DateTimeFormatter.ISO_DATE);
        campaignTotalDataFacade.getCampaignTotalData(email,start,end);

        // get Tree
        LocalDate start2 = LocalDate.parse("2025-07-01",DateTimeFormatter.ISO_DATE);
        LocalDate end2 = LocalDate.parse("2025-08-30",DateTimeFormatter.ISO_DATE);
        AllCampaignTypeData data = segTreeCacheManager.checkPeriod(email,start2,end2);

        // getQuery
        Map<String, CampaignAnalysisDto> data2 =
                keywordService.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmail(email,start2,end2);

        System.out.println(data.getCampaignAnalysisDtoMap().toString());
        System.out.println(data2.toString());
    }
}
