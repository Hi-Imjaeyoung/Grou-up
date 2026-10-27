package growup.spring.springserver.facade;

import growup.spring.springserver.campaign.facade.CampaignTotalDataFacade;
import growup.spring.springserver.global.cache.LazySegmentTreeService;
import growup.spring.springserver.keyword.service.KeywordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CampaignTotalDataFacadeTest {

    private static final String EMAIL = "test@test.com";

    @Mock
    KeywordService keywordService;
    @Mock
    ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("연도를 넘는 조회에서 끝 연도 트리가 없으면 캐시 대신 DB로 조회한다")
    void crossYearQueryFallsBackToDbWhenEndYearIsNotBuilt() {
        LazySegmentTreeService lazySegmentTreeService = new LazySegmentTreeService();
        lazySegmentTreeService.buildTree(EMAIL, 2025, List.of());
        CampaignTotalDataFacade facade = new CampaignTotalDataFacade(keywordService, lazySegmentTreeService, eventPublisher);
        LocalDate start = LocalDate.of(2025, 12, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        facade.getCampaignTotalDataByLazyLoadingTree(EMAIL, start, end);

        verify(keywordService).getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmail(EMAIL, start, end);
    }

    @Test
    @DisplayName("업로드 후 캐시를 무효화하면 사용자의 모든 연도 트리가 지워진다")
    void invalidateCacheRemovesAllYearsOfUser() {
        LazySegmentTreeService lazySegmentTreeService = new LazySegmentTreeService();
        lazySegmentTreeService.buildTree(EMAIL, 2025, List.of());
        lazySegmentTreeService.buildTree(EMAIL, 2026, List.of());
        CampaignTotalDataFacade facade = new CampaignTotalDataFacade(keywordService, lazySegmentTreeService, eventPublisher);

        facade.invalidateCache(EMAIL);

        assertThat(lazySegmentTreeService.isTreeBuild(EMAIL, 2025)).isFalse();
        assertThat(lazySegmentTreeService.isTreeBuild(EMAIL, 2026)).isFalse();
    }
}
