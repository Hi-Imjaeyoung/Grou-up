package growup.spring.springserver.global.listener;

import growup.spring.springserver.global.cache.LazySegmentTreeService;
import growup.spring.springserver.keyword.service.KeywordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CacheBuildEventListenerTest {

    private static final String EMAIL = "test@test.com";
    private static final int YEAR = 2025;

    @Test
    @DisplayName("DB 조회가 실패해도 빌드 중 표시가 지워져 다음 이벤트에서 다시 빌드한다")
    void buildingKeyIsRemovedWhenDbQueryFails() throws Exception {
        LazySegmentTreeService lazySegmentTreeService = new LazySegmentTreeService();
        KeywordService keywordService = mock(KeywordService.class);
        when(keywordService.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByList(
                anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new RuntimeException("DB 연결 실패"))
                .thenReturn(List.of());
        Executor direct = Runnable::run;
        CacheBuildEventListener listener =
                new CacheBuildEventListener(lazySegmentTreeService, keywordService, direct, direct);

        listener.handleTreeBuildEvent(new TreeBuildEvent(EMAIL, YEAR));
        verify(keywordService, timeout(3000).times(1))
                .getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByList(anyString(), any(), any());

        listener.handleTreeBuildEvent(new TreeBuildEvent(EMAIL, YEAR));

        assertThat(waitUntilBuilt(lazySegmentTreeService, 3000)).isTrue();
    }

    private boolean waitUntilBuilt(LazySegmentTreeService service, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (service.isTreeBuild(EMAIL, YEAR)) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }
}
