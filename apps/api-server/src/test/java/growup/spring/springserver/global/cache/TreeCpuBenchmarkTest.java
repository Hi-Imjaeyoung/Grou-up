package growup.spring.springserver.global.cache;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static growup.spring.springserver.campaign.domain.QCampaign.campaign;
import static growup.spring.springserver.keyword.domain.QKeyword.keyword;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class TreeCpuBenchmarkTest {

    private LazySegmentTreeService lazySegmentTreeService;
    private List<Tuple> dummyFullYearData;

    @Param({"10", "30", "60", "90", "180"})
    private int updateDays;

    private List<Tuple> dummyUpdateData;

    @Setup(Level.Trial)
    public void setUp() {
        lazySegmentTreeService = new LazySegmentTreeService();
        dummyFullYearData = createDummyFullYearData();
        dummyUpdateData = createDummyUpdateData(updateDays);

        // 부분 업데이트를 위해 1년치 트리를 메모리에 미리 세팅
        lazySegmentTreeService.buildTree("test@test.com", 2026, dummyFullYearData);
    }

    @Benchmark
    public void testFullBuild() {
        // O(N) 전체 재빌드 측정
        lazySegmentTreeService.buildTree("test@test.com", 2025, dummyFullYearData);
    }

    @Benchmark
    public void testPartialUpdate() {
        // O(T log N) 부분 업데이트 측정
        lazySegmentTreeService.updateTreeByPeriodData("test@test.com", dummyUpdateData);
    }

    // 💡 1. 1년치 더미 데이터 생성 (Mock 제거)
    public List<Tuple> createDummyFullYearData() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);
        List<Tuple> list = new ArrayList<>();
        LocalDate current = start;

        while (!current.isAfter(end)) {
            list.add(createDummyTuple(current));
            current = current.plusDays(1);
        }
        return list;
    }

    // 💡 2. 파라미터(updateDays) 만큼의 더미 데이터 생성 (Mock 제거)
    public List<Tuple> createDummyUpdateData(int period) {
        LocalDate start = LocalDate.of(2026, 1, 1);
        List<Tuple> list = new ArrayList<>();
        int cnt = 0;
        LocalDate current = start;

        while (cnt < period) {
            list.add(createDummyTuple(current));
            current = current.plusDays(1);
            cnt++;
        }
        return list;
    }

    // 💡 3. Mock을 대체할 초경량 DummyTuple 생성 로직
    private Tuple createDummyTuple(LocalDate date) {
        DummyTuple tuple = new DummyTuple();
        tuple.put(keyword.date, date);
        tuple.put(campaign.camAdType, "매출최적화");
        tuple.put(keyword.adCost.sum(), 100.0);
        tuple.put(keyword.adSales.sum(), 500.0);
        return tuple;
    }

    // 💡 4. QueryDSL Tuple 인터페이스의 아주 단순한 가짜 구현체 (Inner Class)
    public static class DummyTuple implements Tuple {
        private final Map<Expression<?>, Object> values = new HashMap<>();

        public <T> void put(Expression<T> expr, T value) {
            values.put(expr, value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Expression<T> expr) {
            return (T) values.get(expr);
        }

        @Override
        public <T> T get(int index, Class<T> type) { return null; }
        @Override
        public Object[] toArray() { return new Object[0]; }
        @Override
        public int size() { return values.size(); }
    }
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(TreeCpuBenchmarkTest.class.getSimpleName()) // 현재 클래스 실행
                .build();

        new Runner(opt).run();
    }
}