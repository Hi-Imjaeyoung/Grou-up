package growup.spring.springserver.campaign;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.exclusionKeyword.domain.ExclusionKeyword;
import growup.spring.springserver.exclusionKeyword.repository.ExclusionKeywordRepository;
import growup.spring.springserver.execution.domain.Execution;
import growup.spring.springserver.execution.repository.ExecutionRepository;
import growup.spring.springserver.keyword.domain.Keyword;
import growup.spring.springserver.keyword.repository.KeywordRepository;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.repository.MarginRepository;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import growup.spring.springserver.memo.domain.Memo;
import growup.spring.springserver.memo.repository.MemoRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;


@DataJpaTest
public class CampaignRepoTest {
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private KeywordRepository keywordRepository;
    @Autowired
    private ExclusionKeywordRepository exclusionKeywordRepository;
    @Autowired
    private MarginRepository marginRepository;
    @Autowired
    private MemoRepository memoRepository;
    @Autowired
    private MarginForCampaignRepository marginForCampaignRepository;
    @Autowired
    private ExecutionRepository executionRepository;
    Member member;

    @BeforeEach
    void setUp(){
        member = memberRepository.save(newMember());
    }

    @Test
    void success_createDB(){
        assertThat(campaignRepository).isNotNull();
    }

    @Test
    void success_saveData(){
        //given
        final Campaign input = newCampaign(member,"test1",1L);
        //when
        final Campaign data = campaignRepository.save(input);
        //then
        assertThat(data.getCampaignId()).isEqualTo(1L);
    }

    @Test
    void success_selectData(){
        //given
        final Campaign input = newCampaign(member,"test1",1L);
        campaignRepository.save(input);
        //when
        final Campaign data = campaignRepository.findByCampaignId(1L).orElseThrow(NullPointerException::new);
        //then
        assertThat(data.getCampaignId()).isEqualTo(1L);
    }

    @Test
    void success_selectByMemberEmail(){
        //given
        final Campaign input1 = newCampaign(member,"test1",1L);
        final Campaign input2 = newCampaign(member,"test2",2L);
        final Campaign input3 = newCampaign(member,"test3",3L);
        //when
        campaignRepository.save(input1);
        campaignRepository.save(input2);
        campaignRepository.save(input3);
        final List<Campaign> data = campaignRepository.findAllByMember(member);
        //then
        assertThat(data.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("findByCampaignIdANDEmail() : Success. 캠패인 단건 조회")
    void test3(){
        //given
        final Campaign input1 = newCampaign(member,"test1",1L);
        campaignRepository.save(input1);
        //whne
        final Campaign result = campaignRepository.findByCampaignIdANDEmail(1L,member.getEmail()).get();
        //then
        assertThat(result.getCampaignId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Campaign Cascade 설정 확인")
    void campaignDelete() {
        // given
        Campaign campaign = campaignRepository.save(newCampaign(newMember(), "campaign1", 1L));
        Keyword keyword = keywordRepository.save(Keyword.builder().campaign(campaign).build());
        campaign.getKeywordList().add(keyword);
        Margin margin = marginRepository.save(Margin.builder().campaign(campaign).build());
        campaign.getMargins().add(margin);
        ExclusionKeyword exclusionKeyword = exclusionKeywordRepository.save(ExclusionKeyword.builder().campaign(campaign).build());
        campaign.getExclusionKeywords().add(exclusionKeyword);
        Execution execution = executionRepository.save(Execution.builder().campaign(campaign).build());
        campaign.getExecutions().add(execution);
        Memo memo = memoRepository.save(Memo.builder().campaign(campaign).build());
        campaign.getMemos().add(memo);
        MarginForCampaign marginForCampaign = marginForCampaignRepository.save(MarginForCampaign.builder().campaign(campaign).build());
        campaign.getMarginForCampaigns().add(marginForCampaign);

        // when
        campaignRepository.deleteById(campaign.getCampaignId());

        // then
        assertFalse(campaignRepository.findById(campaign.getCampaignId()).isPresent());
        assertFalse(keywordRepository.findById(keyword.getId()).isPresent());
        assertFalse(marginRepository.findById(margin.getId()).isPresent());
        assertFalse(exclusionKeywordRepository.findById(exclusionKeyword.getId()).isPresent());
        assertFalse(executionRepository.findById(execution.getId()).isPresent());
        assertFalse(memoRepository.findById(memo.getId()).isPresent());
        assertFalse(marginForCampaignRepository.findById(marginForCampaign.getId()).isPresent());
    }

    public Campaign newCampaign(Member member,String name,Long l){
        return Campaign.builder()
                .member(member)
                .camCampaignName(name)
                .campaignId(l)
                .camAdType("Test")
                .build();
    }

    public Member newMember(){
        return Member.builder().email("test@test.com").build();
    }

}
