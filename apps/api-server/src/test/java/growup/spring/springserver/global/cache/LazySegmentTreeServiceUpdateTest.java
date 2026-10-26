package growup.spring.springserver.global.cache;

import com.querydsl.core.Tuple;
import growup.spring.springserver.campaign.dto.CampaignAnalysisDto;
import growup.spring.springserver.global.cache.TreeCpuBenchmarkTest.DummyTuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static growup.spring.springserver.campaign.domain.QCampaign.campaign;
import static growup.spring.springserver.keyword.domain.QKeyword.keyword;
import static org.assertj.core.api.Assertions.assertThat;

class LazySegmentTreeServiceUpdateTest {

    private static final String EMAIL = "test@test.com";
    private static final String TYPE = "매출최적화";
    private static final int YEAR = 2025;

    private LazySegmentTreeService lazySegmentTreeService;

    @BeforeEach
    void setUp() {
        lazySegmentTreeService = new LazySegmentTreeService();
        // 1/1 ~ 1/10, 하루 광고비 10 / 매출 100
        List<Tuple> daily = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            daily.add(createTuple(LocalDate.of(YEAR, 1, 1).plusDays(i), 10.0, 100.0));
        }
        lazySegmentTreeService.buildTree(EMAIL, YEAR, daily);
    }

    @Test
    @DisplayName("삭제된 데이터로 부분 업데이트하면 해당 일자와 상위 구간에서 값이 빠진다")
    void partialUpdateSubtractsRemovedData() {
        // 1/3, 1/4 는 한 건씩, 1/5 는 같은 날짜 두 건으로 나눠 삭제
        List<Tuple> removed = List.of(
                createTuple(LocalDate.of(YEAR, 1, 3), 10.0, 100.0),
                createTuple(LocalDate.of(YEAR, 1, 4), 10.0, 100.0),
                createTuple(LocalDate.of(YEAR, 1, 5), 4.0, 40.0),
                createTuple(LocalDate.of(YEAR, 1, 5), 6.0, 60.0));

        lazySegmentTreeService.updateTreeByPeriodData(EMAIL, removed);

        assertCostAndSales(query(LocalDate.of(YEAR, 1, 1), LocalDate.of(YEAR, 1, 10)), 70.0, 700.0);
        assertCostAndSales(query(LocalDate.of(YEAR, 1, 1), LocalDate.of(YEAR, 12, 31)), 70.0, 700.0);
        assertCostAndSales(query(LocalDate.of(YEAR, 1, 3), LocalDate.of(YEAR, 1, 3)), 0.0, 0.0);
        assertCostAndSales(query(LocalDate.of(YEAR, 1, 5), LocalDate.of(YEAR, 1, 5)), 0.0, 0.0);
        assertCostAndSales(query(LocalDate.of(YEAR, 1, 1), LocalDate.of(YEAR, 1, 2)), 20.0, 200.0);
    }

    @Test
    @DisplayName("빌드되지 않은 연도의 삭제 데이터는 무시한다")
    void partialUpdateIgnoresUnbuiltYear() {
        lazySegmentTreeService.updateTreeByPeriodData(EMAIL,
                List.of(createTuple(LocalDate.of(YEAR + 1, 1, 1), 10.0, 100.0)));

        assertThat(lazySegmentTreeService.isTreeBuild(EMAIL, YEAR + 1)).isFalse();
        assertCostAndSales(query(LocalDate.of(YEAR, 1, 1), LocalDate.of(YEAR, 1, 10)), 100.0, 1000.0);
    }

    private void assertCostAndSales(CampaignAnalysisDto dto, double adCost, double adSales) {
        assertThat(dto.getAdCost()).isEqualTo(adCost);
        assertThat(dto.getAdSales()).isEqualTo(adSales);
    }

    private CampaignAnalysisDto query(LocalDate start, LocalDate end) {
        return lazySegmentTreeService
                .getCachedOrSelectAllCampaignTypeDataByPeriod(EMAIL, start, end)
                .getCampaignAnalysisDtoMap()
                .get(TYPE);
    }

    private Tuple createTuple(LocalDate date, double adCost, double adSales) {
        DummyTuple tuple = new DummyTuple();
        tuple.put(keyword.date, date);
        tuple.put(campaign.camAdType, TYPE);
        tuple.put(keyword.adCost.sum(), adCost);
        tuple.put(keyword.adSales.sum(), adSales);
        return tuple;
    }
}
