package growup.spring.springserver.campaign.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.global.config.CacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.mockito.Mockito.*;

@SpringJUnitConfig(classes = {CacheConfig.class, CampaignService.class}) // 스프링 최소 기능 기동
class CampaignCacheSliceTest {

    @Autowired
    private CampaignService campaignService;

    @MockBean
    private CampaignRepository campaignRepository;

    @Test
    @DisplayName("getMyCampaign 캐시 적용 확인: 동일 파라미터로 2번 호출 시 DB 접근은 1번만 발생")
    void getMyCampaign_Cache_Test() {
        // given
        Long campaignId = 1L;
        String email = "test@test.com";
        Campaign mockCampaign = Campaign.builder()
                .campaignId(campaignId)
                .camCampaignName("Test Campaign")
                .build();

        doReturn(Optional.of(mockCampaign))
                .when(campaignRepository).findByCampaignIdANDEmail(campaignId, email);

        // when
        campaignService.getMyCampaign(campaignId, email);
        campaignService.getMyCampaign(campaignId, email);

        // then
        verify(campaignRepository, times(1)).findByCampaignIdANDEmail(campaignId, email);
    }
}
