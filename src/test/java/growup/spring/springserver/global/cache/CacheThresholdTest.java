package growup.spring.springserver.global.cache;

import com.querydsl.core.Tuple;
import growup.spring.springserver.keyword.service.KeywordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static growup.spring.springserver.campaign.domain.QCampaign.campaign;
import static growup.spring.springserver.keyword.domain.QKeyword.keyword;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class CacheThresholdTest {

    @Autowired
    private LazySegmentTreeService lazySegmentTreeService;
    @MockBean
    private KeywordService keywordService;

    private final String TEST_EMAIL = "user101@test.com";
    private final LocalDate START_DATE = LocalDate.of(2025, 6, 1);
    private final LocalDate END_DATE = LocalDate.of(2025, 12, 30); // 약 180일치 (N=180)

    @BeforeEach
    void setUp() {
        lazySegmentTreeService.removeAllTreeDataByEmail(TEST_EMAIL);
        lazySegmentTreeService.getCachedOrSelectAllCampaignTypeDataByPeriod(TEST_EMAIL, START_DATE, END_DATE);
        System.out.println("캐시 초기화 및 베이스 트리 세팅 완료!");
    }

    @BeforeEach
    void setUpMock() {
        given(keywordService.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByList(
                any(), any(), any()
        )).willAnswer(invocation -> {
            LocalDate start = invocation.getArgument(1);
            LocalDate end = invocation.getArgument(2);
            return createDummyTuples(start, end);
        });
    }

    public List<Tuple> createDummyTuples(LocalDate start, LocalDate end) {
        List<Tuple> list = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            Tuple mockTuple = mock(Tuple.class);
            when(mockTuple.get(keyword.date)).thenReturn(current);
            when(mockTuple.get(campaign.camAdType)).thenReturn("매출최적화");
            when(mockTuple.get(keyword.adCost.sum())).thenReturn(100.0);
            when(mockTuple.get(keyword.adSales.sum())).thenReturn(500.0);
            list.add(mockTuple);
            current = current.plusDays(1);
        }

        return list;
    }

    @ParameterizedTest
    @CsvSource({"5", "10", "22", "30", "40", "50", "60"}) // T값 (삭제 기간)
    @DisplayName("삭제 기간(T)에 따른 Update vs Rebuild 시간 복잡도 역전 검증")
    void measureThreshold(int T) {

        // --- [Given] T일 만큼의 업데이트용 더미 데이터 생성 ---
        Map<LocalDate, AllCampaignTypeData> dummyUpdateMap = new HashMap<>();
        for (int i = 0; i < T; i++) {
            dummyUpdateMap.put(START_DATE.plusDays(i), new AllCampaignTypeData("SA", -1000.0, -1000.0));
        }

        // --- [측정 1. Update (T일치 DB 조회 + O(T log N) 메모리 연산)] ---
        long updateStart = System.nanoTime();

        // 3. 서비스의 update 로직 호출 (메모리 트리 업데이트)
        lazySegmentTreeService.updateTreeByPeriodData(TEST_EMAIL, dummyUpdateMap);

        long updateEnd = System.nanoTime();
        double updateTimeMs = (updateEnd - updateStart) / 1_000_000.0;


        // --- [측정 2. O(N) Rebuild 연산 소요 시간] ---
        lazySegmentTreeService.removeAllTreeDataByEmail(TEST_EMAIL); // 캐시 비우기
        long rebuildStart = System.nanoTime();
        // 다시 조회해서 트리를 재빌드 하도록 유도
        lazySegmentTreeService.getCachedOrSelectAllCampaignTypeDataByPeriod(TEST_EMAIL, START_DATE, END_DATE);
        long rebuildEnd = System.nanoTime();
        double rebuildTimeMs = (rebuildEnd - rebuildStart) / 1_000_000.0;

        // --- [결과 출력] ---
        System.out.println("======================================");
        System.out.printf("[T = %2d] Update  (O(T log N)) : %.4f ms%n", T, updateTimeMs);
        System.out.printf("[T = %2d] Rebuild (O(N) + DB I/O): %.4f ms%n", T, rebuildTimeMs);

        if (updateTimeMs < rebuildTimeMs) {
            System.out.println("Update가 더 빠름 (기존 트리 살리는 게 이득!)");
        } else {
            System.out.println("Rebuild가 더 빠름 (임계값 초과! 트리 부수고 다시 짓는 게 이득!)");
        }
        System.out.println("======================================\n");
    }
}
