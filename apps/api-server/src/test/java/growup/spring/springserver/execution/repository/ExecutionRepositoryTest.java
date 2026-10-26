package growup.spring.springserver.execution.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.execution.domain.Execution;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ExecutionRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    Member member;
    Campaign campaign, campaign2;
    Execution execution1, execution2,execution3;

    @BeforeEach
    void setup() {
        member = memberRepository.save(newMember());
        campaign = campaignRepository.save(newCampaign(member,1L));
        campaign2 = campaignRepository.save(newCampaign(member,2L));

    }

    @Test
    void success_createDB() {
        assertThat(executionRepository).isNotNull();
    }

    @Test
    @DisplayName("findByCampaignCampaignId(): 특정 Campaign ID와 관련된 Execution 반환")
    void findByCampaignCampaignIdReturnsExecutions() {
        // Given
        execution1 = executionRepository.save(newExecution(campaign, 91130550615L, "블랙", "안경 케이스 - 블랙"));
        execution2 = executionRepository.save(newExecution(campaign, 91130550622L, "화이트", "안경 케이스 - 화이트"));
        execution3 = executionRepository.save(newExecution(campaign2, 91130550623L, "블루", "안경 케이스 - 블루"));

        // When
        List<Long> result = executionRepository.findExecutionIdsByCampaignId(campaign.getCampaignId());

        // Then
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("findByCampaignCampaignId(): 없는 값")
    void findByCampaignCampaignIdReturnsEmptyOptional() {
        // When
        List<Long> result = executionRepository.findExecutionIdsByCampaignId(999999L);

        // Then
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("findByCampaignId() : 특정 캠페인 ID 완 관련된 Execution 객체 변환 ")
    void findByCampaignId() {
        // given
        execution1 = executionRepository.save(newExecution(campaign, 91130550615L, "블랙", "안경 케이스 - 블랙"));
        execution2 = executionRepository.save(newExecution(campaign, 91130550622L, "화이트", "안경 케이스 - 화이트"));
        execution3 = executionRepository.save(newExecution(campaign2, 91130550623L, "블루", "안경 케이스 - 블루"));
        //when
        List<Execution> result = executionRepository.findExecutionByCampaignId(campaign.getCampaignId());
        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(execution1);
        assertThat(result.get(1)).isEqualTo(execution2);
    }

    @Test
    @DisplayName("findByCampaign_CampaignIdAndExeId() :특정 캠페인 관련 Execution 객체 변환")
    void findByCampaign_CampaignIdAndExeId_Success() {

        // given
        execution1 = executionRepository.save(newExecution(campaign, 91130550615L, "블랙", "안경 케이스 - 블랙"));
        execution3 = executionRepository.save(newExecution(campaign, 91130550623L, "블루", "안경 케이스 - 블루"));
        execution3 = executionRepository.save(newExecution(campaign2, 91130550623L, "블루", "안경 케이스 - 블루"));
        // when

        Optional<Execution> result = executionRepository.findByCampaign_CampaignIdAndExeId(campaign.getCampaignId(), 91130550615L);
        //then

        assertThat(result).isPresent();
        assertThat(result.get().getCampaign()).isEqualTo(campaign);
        assertThat(result.get().getExeId()).isEqualTo(91130550615L);
    }

//    @Test
//    @DisplayName("findByCampaignIdAndExeId() : Success 1. 조회한 값이 없을 떄")
//    void test3_1(){
//        // given
//        execution1 = executionRepository.save(newExecution(campaign, 91130550615L, "블랙", "안경 케이스 - 블랙"));
//        execution2 = executionRepository.save(newExecution(campaign, 91130550622L, "화이트", "안경 케이스 - 화이트"));
//        execution3 = executionRepository.save(newExecution(campaign2, 91130550623L, "블루", "안경 케이스 - 블루"));
//        // when
//        List<Execution> result = executionRepository.findByCampaignIdAndExeId(3L,List.of(91130550615L,91130550622L));
//        // then
//        assertThat(result).hasSize(0);
//    }

//    @Test
//    @DisplayName("findByCampaignIdAndExeId() : Success")
//    void test3_2(){
//        // given
//        execution1 = executionRepository.save(newExecution(campaign, 91130550615L, "블랙", "안경 케이스 - 블랙"));
//        execution2 = executionRepository.save(newExecution(campaign, 91130550622L, "화이트", "안경 케이스 - 화이트"));
//        execution3 = executionRepository.save(newExecution(campaign2, 91130550623L, "블루", "안경 케이스 - 블루"));
//        // when
//        List<Execution> result = executionRepository.findByCampaignIdAndExeId(1L,List.of(91130550615L,91130550622L));
//        // then
//        assertThat(result).hasSize(2);
//    }


    public Member newMember() {
        return Member.builder().email("test@test.com").build();
    }

    public Campaign newCampaign(Member member,Long campaignId) {
        return Campaign.builder()
                .member(member)
                .camCampaignName("방한 마스크")
                .campaignId(campaignId)
                .camAdType("Test")
                .build();
    }

    public Execution newExecution(Campaign campaign, long l, String detail, String name) {
        return Execution.builder()
                .exeId(l)
                .exeDetailCategory(detail)
                .exeProductName(name)
                .campaign(campaign)
                .build();
    }
}