package growup.spring.springserver.facade;

import growup.spring.springserver.campaign.facade.CampaignDeleteFacade;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.campaignoptiondetails.service.CampaignOptionDetailsService;
import growup.spring.springserver.exclusionKeyword.service.ExclusionKeywordService;
import growup.spring.springserver.execution.service.ExecutionService;
import growup.spring.springserver.global.cache.LazySegmentTreeService;
import growup.spring.springserver.keyword.service.KeywordService;
import growup.spring.springserver.keywordBid.service.KeywordBidService;
import growup.spring.springserver.margin.service.MarginService;
import growup.spring.springserver.marginforcampaign.service.MarginForCampaignService;
import growup.spring.springserver.memo.service.MemoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CampaignDeleteFacadeTest {
    @InjectMocks
    CampaignDeleteFacade campaignDeleteFacade;

    @Mock
    CampaignService campaignService;
    @Mock
    KeywordService keywordService;
    @Mock
    MarginService marginService;
    @Mock
    MemoService memoService;
    @Mock
    ExecutionService executionService;
    @Mock
    CampaignOptionDetailsService campaignOptionDetailsService;
    @Mock
    ExclusionKeywordService exclusionKeywordService;
    @Mock
    KeywordBidService keywordBidService;
    @Mock
    MarginForCampaignService marginForCampaignService;
    @Mock
    LazySegmentTreeService lazySegmentTreeService;

    @Test
    @DisplayName("1. 임계값을 넘으면, 기간 내 캠패인 관련 모든 데이터를 삭제 후 트리를 재빌드한다.")
    void name1(){

    }


    @Test
    @DisplayName("2.임계값을 준수하면, 기간 내 캠패인 관련 모든 데이터 추출하여 트리를 수정하고 데이터를 삭제한다.")
    void name2(){
    }
}
