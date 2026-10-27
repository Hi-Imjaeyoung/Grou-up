package growup.spring.springserver.global.cache;

import com.querydsl.core.Tuple;
import growup.spring.springserver.global.cache.TreeCpuBenchmarkTest.DummyTuple;
import org.openjdk.jmh.annotations.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static growup.spring.springserver.campaign.domain.QCampaign.campaign;
import static growup.spring.springserver.keyword.domain.QKeyword.keyword;

/**
 * 구간 합 조회: 세그먼트 트리 vs 일별 데이터 선형 합산.
 * 1년(365일) × 광고 유형 3개 데이터를 메모리에 올려 둔 상태에서 조회 비용만 비교한다.
 * 실행: ./gradlew jmh -Pbench=RangeQueryBenchmarkTest
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class RangeQueryBenchmarkTest {

    private static final String EMAIL = "test@test.com";
    private static final int YEAR = 2025;
    private static final String[] TYPES = {"매출최적화", "수동 성과형", "간편 매출 스타트"};

    @Param({"7", "30", "90", "180", "365"})
    private int rangeDays;

    private LazySegmentTreeService lazySegmentTreeService;
    // 단순 대안: 일별 데이터만 캐싱 (key: 연초부터의 일수)
    private Map<Integer, AllCampaignTypeData> dailyCache;
    private LocalDate start;
    private LocalDate end;

    @Setup(Level.Trial)
    public void setUp() {
        List<Tuple> yearData = createYearData();
        lazySegmentTreeService = new LazySegmentTreeService();
        lazySegmentTreeService.buildTree(EMAIL, YEAR, yearData);

        dailyCache = new HashMap<>();
        for (Tuple tuple : yearData) {
            int day = lazySegmentTreeService.convertLocalDateToCount(tuple.get(keyword.date));
            AllCampaignTypeData data = new AllCampaignTypeData(
                    tuple.get(campaign.camAdType), tuple.get(keyword.adCost.sum()), tuple.get(keyword.adSales.sum()));
            dailyCache.merge(day, data, AllCampaignTypeData::add);
        }

        // 트리 노드 경계에 맞지 않도록 1/10 부터 시작 (365일은 연간 전체)
        start = rangeDays == 365 ? LocalDate.of(YEAR, 1, 1) : LocalDate.of(YEAR, 1, 10);
        end = start.plusDays(rangeDays - 1);
    }

    @Benchmark
    public AllCampaignTypeData segmentTree() {
        return lazySegmentTreeService.getCachedOrSelectAllCampaignTypeDataByPeriod(EMAIL, start, end);
    }

    @Benchmark
    public AllCampaignTypeData linearSum() {
        int from = lazySegmentTreeService.convertLocalDateToCount(start);
        int to = lazySegmentTreeService.convertLocalDateToCount(end);
        AllCampaignTypeData result = new AllCampaignTypeData();
        for (int day = from; day <= to; day++) {
            result.add(dailyCache.get(day));
        }
        return result;
    }

    private List<Tuple> createYearData() {
        List<Tuple> list = new ArrayList<>();
        for (LocalDate date = LocalDate.of(YEAR, 1, 1); date.getYear() == YEAR; date = date.plusDays(1)) {
            for (String type : TYPES) {
                DummyTuple tuple = new DummyTuple();
                tuple.put(keyword.date, date);
                tuple.put(campaign.camAdType, type);
                tuple.put(keyword.adCost.sum(), 100.0);
                tuple.put(keyword.adSales.sum(), 500.0);
                list.add(tuple);
            }
        }
        return list;
    }
}
