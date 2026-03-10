package growup.spring.springserver.campaignDeleteDto;

import growup.spring.springserver.campaign.dto.CampaignDeleteDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CampaignDeleteDtoTest {

    @Test
    @DisplayName("캠페인 삭제 기간이 임계값(예: 3개월) 이상이면 true를 반환한다")
    void shouldReturnTrueWhenDatePeriodExceedsThreshold(){
        CampaignDeleteDto dto = CampaignDeleteDto.builder()
                .start(LocalDate.of(2026,1,1))
                .end(LocalDate.of(2026,4,1))
                .campaignIds(List.of(1L,2L))
                .build();
        boolean result = dto.checkThreshold();
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("캠페인 삭제 기간이 임계값(예: 3개월) 이하면 false를 반환한다")
    void shouldReturnFalseWhenDatePeriodInThreshold(){
        CampaignDeleteDto dto = CampaignDeleteDto.builder()
                .start(LocalDate.of(2026,1,1))
                .end(LocalDate.of(2026,2,1))
                .campaignIds(List.of(1L,2L))
                .build();
        boolean result = dto.checkThreshold();
        assertThat(result).isFalse();
    }
}
