package growup.spring.springserver.campaignDeleteDto;

import growup.spring.springserver.campaign.dto.CampaignDeleteDto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CampaignDeleteDtoTest {

    @Test
    @DisplayName("삭제 기간이 짧으면 부분 업데이트를 고른다")
    void shortPeriodUsesPartialUpdate() {
        assertThat(dto(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 10)).checkThreshold()).isTrue();
        assertThat(dto(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 4, 29)).checkThreshold()).isTrue(); // 60일
    }

    @Test
    @DisplayName("삭제 기간이 길면 전체 재빌드를 고른다")
    void longPeriodUsesFullRebuild() {
        assertThat(dto(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 4, 30)).checkThreshold()).isFalse(); // 61일
        assertThat(dto(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)).checkThreshold()).isFalse();
    }

    @Test
    @DisplayName("연도를 넘는 삭제는 전체 재빌드를 고른다")
    void crossYearUsesFullRebuild() {
        assertThat(dto(LocalDate.of(2025, 12, 30), LocalDate.of(2026, 1, 2)).checkThreshold()).isFalse();
    }

    private CampaignDeleteDto dto(LocalDate start, LocalDate end) {
        return CampaignDeleteDto.builder().start(start).end(end).campaignIds(List.of(1L)).build();
    }
}
