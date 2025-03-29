package growup.spring.springserver.memo;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.memo.domain.Memo;
import growup.spring.springserver.memo.repository.MemoRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.StatusResultMatchersExtensionsKt.isEqualTo;

@DataJpaTest
public class MemoRepositoryTest {
    @Autowired
    private MemoRepository memoRepository;
    @Autowired
    private CampaignRepository campaignRepository;
    private Campaign campaign, campaign2;

    @BeforeEach
    void setUp(){
        campaign = Campaign.builder()
                .campaignId(1L)
                .camCampaignName("testCampaign1")
                .build();
        campaign = campaignRepository.save(campaign);
        campaign2 = Campaign.builder()
                .campaignId(2L)
                .camCampaignName("testCampaign2")
                .build();
        campaign2 = campaignRepository.save(campaign2);
    }

    @DisplayName("저장")
    @Test
    void save(){
        Memo memo = makeMemo("contents..",campaign);
        final Memo result = memoRepository.save(memo);
        assertThat(result.getContents()).isEqualTo("contents..");
        assertThat(result.getDate()).isEqualTo(LocalDate.now());
    }
    @DisplayName("해당 캠패인 메모 조회")
    @Test
    void selectAboutCampaign(){
        memoRepository.save(makeMemo("contents",campaign));
        memoRepository.save(makeMemo("contents",campaign));
        memoRepository.save(makeMemo("contents",campaign));
        memoRepository.save(makeMemo("contents",campaign));
        memoRepository.save(makeMemo("this memo will not select!",campaign2));

        final List<Memo> result = memoRepository.findAllByCampaign(campaign);

        for(Memo memo : result){
            assertThat(memo.getContents()).isEqualTo("contents");
        }

    }

    @DisplayName("메모 단건 조회")
    @Test
    void selectSingleMemo(){
        memoRepository.save(makeMemo("contents",campaign));
        memoRepository.save(makeMemo("contents",campaign));
        memoRepository.save(makeMemo("contents",campaign));
        Memo memo = memoRepository.save(makeMemo("find this memo!",campaign));
        final Optional<Memo> result = memoRepository.findById(memo.getId());
        assertThat(result.isPresent()).isEqualTo(true);
        assertThat(result.get().getContents()).isEqualTo("find this memo!");
    }

    @DisplayName("삭제")
    @Test
    void delete(){
        Memo savedMemo = memoRepository.save(makeMemo("update this memo to success update",campaign));
        final int result = memoRepository.deleteMemoById(savedMemo.getId());
//        final Optional<Memo> result = memoRepository.findById(savedMemo.getId());
//        assertThat(result.isPresent()).isEqualTo(false);
        assertThat(result).isEqualTo(1);
    }
    @DisplayName("수정")
    @Test
    void update(){
        memoRepository.save(makeMemo("contents",campaign));
        memoRepository.save(makeMemo("contents",campaign));
        memoRepository.save(makeMemo("contents",campaign2));
        Memo savedMemo = memoRepository.save(makeMemo("update this memo to success update",campaign));

        Memo memo = memoRepository.findById(savedMemo.getId()).get();
        memo.updateContents("success update");

        final Optional<Memo> result = memoRepository.findById(memo.getId());
        assertThat(result.isPresent()).isEqualTo(true);
        assertThat(result.get().getContents()).isEqualTo("success update");
    }

    @Test
    @DisplayName("deleteByCampaignIdAndDate()")
    @Transactional
    void deleteByCampaignIdAndDat(){
        //when
        LocalDate start = LocalDate.parse("2025-03-01", DateTimeFormatter.ISO_DATE);
        LocalDate end = LocalDate.parse("2025-03-29",DateTimeFormatter.ISO_DATE);
        LocalDate includeDate = LocalDate.parse("2025-03-20",DateTimeFormatter.ISO_DATE);
        LocalDate  excludeDate = LocalDate.parse("2025-04-01",DateTimeFormatter.ISO_DATE);
        memoRepository.save(makeMemo("memo1",includeDate,campaign));
        memoRepository.save(makeMemo("memo2",excludeDate,campaign));
        //given
        final int result = memoRepository.deleteByCampaignIdAndDate(start,end,campaign.getCampaignId());
        assertThat(result).isEqualTo(1);
    }

    private Memo makeMemo(String contents4,Campaign campaign) {
        return Memo.builder()
                .date(LocalDate.now())
                .campaign(campaign)
                .contents(contents4)
                .build();
    }

    private Memo makeMemo(String contents4,LocalDate localDate,Campaign campaign) {
        return Memo.builder()
                .date(localDate)
                .campaign(campaign)
                .contents(contents4)
                .build();
    }
}
