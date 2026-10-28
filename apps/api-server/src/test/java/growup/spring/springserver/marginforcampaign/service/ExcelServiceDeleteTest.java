package growup.spring.springserver.marginforcampaign.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.dto.ExcelUploadContext;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExcelServiceDeleteTest {

    @Mock
    MarginForCampaignRepository marginForCampaignRepository;
    @Mock
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("엑셀에 없는 기존 옵션은 범위 DELETE 대신 PK로 지운다 (동시 업로드 gap lock 데드락 방지)")
    void deletesMissingOptionsByPrimaryKey() throws IOException {
        Campaign campaign1 = Campaign.builder().campaignId(1L).build();
        Campaign campaign2 = Campaign.builder().campaignId(2L).build();
        Map<String, MarginForCampaign> existing = new HashMap<>();
        existing.put("A", mfc(10L, "A", campaign1));
        existing.put("B", mfc(11L, "B", campaign1));
        existing.put("C", mfc(12L, "C", campaign1));
        ExcelUploadContext context = new ExcelUploadContext("test@test.com",
                Map.of(1L, campaign1, 2L, campaign2), Map.of(1L, existing));
        context.addOptionNameFromExcel(1L, "A");
        context.addOptionNameFromExcel(1L, "D");
        context.addOptionNameFromExcel(2L, "E"); // 기존 옵션이 없는 캠페인은 지울 것도 없다

        new ExcelService(marginForCampaignRepository, jdbcTemplate).processUploadedExcel(context);

        ArgumentCaptor<List<Long>> ids = ArgumentCaptor.forClass(List.class);
        verify(marginForCampaignRepository, times(1)).deleteAllByIdInBatch(ids.capture());
        assertThat(ids.getValue()).containsExactlyInAnyOrder(11L, 12L);
        verify(marginForCampaignRepository, never()).deleteNotIncludeOptionName(anyList(), anyString(), anyLong());
    }

    private MarginForCampaign mfc(Long id, String name, Campaign campaign) {
        return MarginForCampaign.builder().id(id).mfcProductName(name).campaign(campaign).build();
    }
}
