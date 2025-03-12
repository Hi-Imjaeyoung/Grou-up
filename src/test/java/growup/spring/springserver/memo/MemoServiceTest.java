package growup.spring.springserver.memo;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.exception.memo.MemoNotFoundException;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.memo.domain.Memo;
import growup.spring.springserver.memo.dto.MemoRequestDto;
import growup.spring.springserver.memo.dto.MemoUpdateRequestDto;
import growup.spring.springserver.memo.repository.MemoRepository;
import growup.spring.springserver.memo.service.MemoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class MemoServiceTest {
    @InjectMocks
    private MemoService memoService;

    @Mock
    private MemoRepository memoRepository;

    @DisplayName("Memo 생성")
    @Test
    void makeMemo(){
        MemoRequestDto requestDto = getMemoRequestDto(null,"im so happy",LocalDate.now());
        Memo savedMemo = getSavedMemo(1L,"im so happy",LocalDate.now());
        doReturn(savedMemo).when(memoRepository).save(any(Memo.class));

        final Memo result = memoService.makeMemo(getCampaign(),requestDto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCampaign().getCampaignId()).isEqualTo(1L);
    }

    @DisplayName("Campaign Memo 조회")
    @Test
    void getMemoAboutCampaign(){
        doReturn(List.of(getSavedMemo(1L,"memo1",LocalDate.now()),
                getSavedMemo(2L,"memo2",LocalDate.now()),
                getSavedMemo(3L,"memo3",LocalDate.now()))).when(memoRepository).findAllByCampaign(any(Campaign.class));
        final List<Memo> result = memoService.getMemoAboutCampaign(getCampaign());
        assertThat(result).hasSize(3);
    }

    @DisplayName("Memo 수정")
    @Test
    void updateMemo(){
        doReturn(Optional.of(getSavedMemo(1L,"contents",LocalDate.now()))).when(memoRepository).findById(1L);
        final Memo result = memoService.updateMemo(getMemoUpdateRequestDto(1L,"updateContents"));
        assertThat(result.getContents()).isEqualTo("updateContents");
    }
    @DisplayName("Memo 수정 실패 : 해당 메모가 없는 경우")
    @Test
    void updateMeme_fail(){
        doReturn(Optional.empty()).when(memoRepository).findById(1L);
        final MemoNotFoundException result = assertThrows(MemoNotFoundException.class,()->
                memoService.updateMemo(getMemoUpdateRequestDto(1L,"updateMemo"))
        );
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.MEMO_NOT_FOUND);
    }

    @DisplayName("Memo 삭제")
    @Test
    void deleteMemo(){
        doReturn(1).when(memoRepository).deleteMemoById(1L);
        final int result = memoService.deleteMemo(1L);
        assertThat(result).isEqualTo(1);
    }

    private Campaign getCampaign(){
        return Campaign.builder()
                .campaignId(1L)
                .camCampaignName("testCamp..")
                .build();
    }

    private Memo getSavedMemo(Long id,String contents, LocalDate localDate) {
        return Memo.builder()
                .id(id)
                .campaign(getCampaign())
                .date(localDate)
                .contents(contents)
                .build();
    }

    private static MemoRequestDto getMemoRequestDto(Long id,String contents,LocalDate localDate) {
        return MemoRequestDto.builder()
                .id(id)
                .date(localDate)
                .contents(contents)
                .build();
    }

    private static MemoUpdateRequestDto getMemoUpdateRequestDto(Long id, String contents){
        return MemoUpdateRequestDto.builder()
                .memoId(id)
                .contents(contents)
                .build();
    }
}
