package growup.spring.springserver.global.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.querydsl.core.Tuple;
import growup.spring.springserver.global.cache.TreeCpuBenchmarkTest.DummyTuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.GraphLayout;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static growup.spring.springserver.campaign.domain.QCampaign.campaign;
import static growup.spring.springserver.keyword.domain.QKeyword.keyword;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 1년 트리 한 개의 메모리 사용량을 JOL 로 측정한다. 결과는 표준 출력으로 남긴다.
 * 실행: ./gradlew test --tests '*SegmentTreeMemoryTest' -i
 */
class SegmentTreeMemoryTest {

    private static final String EMAIL = "test@test.com";
    private static final int YEAR = 2025;
    private static final String[] TYPES = {"매출최적화", "수동 성과형", "간편 매출 스타트"};

    @Test
    @DisplayName("1년 트리 1개(광고 유형 1·3개)의 retained size 를 측정한다")
    void measureOneYearTree() throws Exception {
        for (int typeCount : new int[]{1, 3}) {
            LazySegmentTreeService service = new LazySegmentTreeService();
            service.buildTree(EMAIL, YEAR, createYearData(typeCount));

            // Caffeine 내부 람다(hidden class)는 JOL 이 순회하지 못하므로, 사용자별 트리 객체만 측정한다
            Object userTree = getUserTree(service, EMAIL);
            GraphLayout layout = GraphLayout.parseInstance(userTree);
            long bytes = layout.totalSize();

            System.out.printf("[SegmentTreeMemory] types=%d, 1 tree=%,d bytes (%.1f KB), objects=%,d, 5000 users ≈ %.1f MB%n",
                    typeCount, bytes, bytes / 1024.0, layout.totalCount(), bytes * 5000 / 1024.0 / 1024.0);
            System.out.println(layout.toFootprint());

            assertThat(bytes).isPositive();
        }
    }

    @SuppressWarnings("unchecked")
    private Object getUserTree(LazySegmentTreeService service, String email) throws Exception {
        Field field = LazySegmentTreeService.class.getDeclaredField("lazyCacheTree");
        field.setAccessible(true);
        return ((Cache<String, Object>) field.get(service)).getIfPresent(email);
    }

    private List<Tuple> createYearData(int typeCount) {
        List<Tuple> list = new ArrayList<>();
        for (LocalDate date = LocalDate.of(YEAR, 1, 1); date.getYear() == YEAR; date = date.plusDays(1)) {
            for (int i = 0; i < typeCount; i++) {
                DummyTuple tuple = new DummyTuple();
                tuple.put(keyword.date, date);
                tuple.put(campaign.camAdType, TYPES[i]);
                tuple.put(keyword.adCost.sum(), 100.0);
                tuple.put(keyword.adSales.sum(), 500.0);
                list.add(tuple);
            }
        }
        return list;
    }
}
