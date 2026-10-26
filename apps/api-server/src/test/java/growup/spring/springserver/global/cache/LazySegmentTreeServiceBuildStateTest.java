package growup.spring.springserver.global.cache;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import growup.spring.springserver.global.cache.TreeCpuBenchmarkTest.DummyTuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static growup.spring.springserver.campaign.domain.QCampaign.campaign;
import static growup.spring.springserver.keyword.domain.QKeyword.keyword;
import static org.assertj.core.api.Assertions.assertThat;

class LazySegmentTreeServiceBuildStateTest {

    private static final String EMAIL = "test@test.com";
    private static final int YEAR = 2025;

    private LazySegmentTreeService lazySegmentTreeService;

    @BeforeEach
    void setUp() {
        lazySegmentTreeService = new LazySegmentTreeService();
    }

    @Test
    @DisplayName("빌드가 끝나기 전에는 isTreeBuild 가 false 다")
    void treeIsNotVisibleWhileBuilding() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Tuple blockingTuple = new BlockingTuple(createTuple(LocalDate.of(YEAR, 1, 1)), entered, release);

        CompletableFuture<Void> build = CompletableFuture.runAsync(
                () -> lazySegmentTreeService.buildTree(EMAIL, YEAR, List.of(blockingTuple)));
        try {
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

            assertThat(lazySegmentTreeService.isTreeBuild(EMAIL, YEAR)).isFalse();
        } finally {
            release.countDown();
            build.get(5, TimeUnit.SECONDS);
        }
        assertThat(lazySegmentTreeService.isTreeBuild(EMAIL, YEAR)).isTrue();
    }

    @Test
    @DisplayName("빌드되지 않은 연도를 조회해도 그 연도가 빌드된 것으로 바뀌지 않는다")
    void queryingUnbuiltYearDoesNotMarkItBuilt() {
        lazySegmentTreeService.getCachedOrSelectAllCampaignTypeDataByPeriod(
                EMAIL, LocalDate.of(YEAR, 1, 1), LocalDate.of(YEAR, 1, 10));

        assertThat(lazySegmentTreeService.isTreeBuild(EMAIL, YEAR)).isFalse();
    }

    private Tuple createTuple(LocalDate date) {
        DummyTuple tuple = new DummyTuple();
        tuple.put(keyword.date, date);
        tuple.put(campaign.camAdType, "매출최적화");
        tuple.put(keyword.adCost.sum(), 10.0);
        tuple.put(keyword.adSales.sum(), 100.0);
        return tuple;
    }

    // DB 결과를 읽는 순간(빌드 도중) 멈춰서, 빌드 중 상태를 관찰할 수 있게 한다
    private static class BlockingTuple implements Tuple {
        private final Tuple delegate;
        private final CountDownLatch entered;
        private final CountDownLatch release;

        BlockingTuple(Tuple delegate, CountDownLatch entered, CountDownLatch release) {
            this.delegate = delegate;
            this.entered = entered;
            this.release = release;
        }

        @Override
        public <T> T get(Expression<T> expr) {
            entered.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return delegate.get(expr);
        }

        @Override
        public <T> T get(int index, Class<T> type) { return delegate.get(index, type); }
        @Override
        public Object[] toArray() { return delegate.toArray(); }
        @Override
        public int size() { return delegate.size(); }
    }
}
