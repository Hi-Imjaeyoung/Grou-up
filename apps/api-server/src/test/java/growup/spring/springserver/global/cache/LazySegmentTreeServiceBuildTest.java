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

class LazySegmentTreeServiceBuildTest {

    private static final String EMAIL = "test@test.com";
    private static final String TYPE = "매출최적화";
    private static final int YEAR = 2025;

    private LazySegmentTreeService lazySegmentTreeService;

    @BeforeEach
    void setUp() {
        lazySegmentTreeService = new LazySegmentTreeService();
        // 1/1 ~ 1/10, 하루 광고비 10 / 매출 100
        lazySegmentTreeService.buildTree(EMAIL, YEAR, createDailyData(LocalDate.of(YEAR, 1, 1), 10, 10.0, 100.0));
    }

    @Test
    @DisplayName("buildTree 후 단일 일자 조회는 그날 값만 반환한다")
    void singleDayQueryReturnsOnlyThatDay() {
        LocalDate day = LocalDate.of(YEAR, 1, 1);

        CampaignAnalysisDto result = query(day, day);

        assertThat(result.getAdCost()).isEqualTo(10.0);
        assertThat(result.getAdSales()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("buildTree 후 구간 조회는 구간 합계를 반환한다")
    void rangeQueryReturnsSumOfRange() {
        CampaignAnalysisDto result = query(LocalDate.of(YEAR, 1, 1), LocalDate.of(YEAR, 1, 10));

        assertThat(result.getAdCost()).isEqualTo(100.0);
        assertThat(result.getAdSales()).isEqualTo(1000.0);
    }

    @Test
    @DisplayName("buildTree 후 연간 조회는 전체 합계를 반환하고, 반복 조회해도 값이 변하지 않는다")
    void fullYearQueryIsStable() {
        LocalDate start = LocalDate.of(YEAR, 1, 1);
        LocalDate end = LocalDate.of(YEAR, 12, 31);

        CampaignAnalysisDto first = query(start, end);
        CampaignAnalysisDto second = query(start, end);

        assertThat(first.getAdCost()).isEqualTo(100.0);
        assertThat(first.getAdSales()).isEqualTo(1000.0);
        assertThat(second.getAdCost()).isEqualTo(100.0);
        assertThat(second.getAdSales()).isEqualTo(1000.0);
    }

    private CampaignAnalysisDto query(LocalDate start, LocalDate end) {
        return lazySegmentTreeService
                .getCachedOrSelectAllCampaignTypeDataByPeriod(EMAIL, start, end)
                .getCampaignAnalysisDtoMap()
                .get(TYPE);
    }

    private List<Tuple> createDailyData(LocalDate start, int days, double adCost, double adSales) {
        List<Tuple> list = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            DummyTuple tuple = new DummyTuple();
            tuple.put(keyword.date, start.plusDays(i));
            tuple.put(campaign.camAdType, TYPE);
            tuple.put(keyword.adCost.sum(), adCost);
            tuple.put(keyword.adSales.sum(), adSales);
            list.add(tuple);
        }
        return list;
    }
}
