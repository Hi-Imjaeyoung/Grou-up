package growup.spring.springserver.global.cache;

import com.querydsl.core.Tuple;
import growup.spring.springserver.campaign.dto.CampaignAnalysisDto;
import growup.spring.springserver.keyword.service.KeywordService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.LongAdder;

import static growup.spring.springserver.campaign.domain.QCampaign.campaign;
import static growup.spring.springserver.keyword.domain.QKeyword.keyword;

@Service
@Slf4j
@AllArgsConstructor
public class LazySegmentTreeService {
    private final KeywordService keywordService;
    private final LongAdder hitCount = new LongAdder();
    private final LongAdder requestCount = new LongAdder();

    private static class SegmentNodeData {
        // key : My bitPacking
        Map<Integer, AllCampaignTypeData> dataMap = new ConcurrentHashMap<>();

        public boolean containsKey(int key) {
            return dataMap.containsKey(key);
        }

        public AllCampaignTypeData getCampaignAnalysisDto(int key) {
            return dataMap.get(key);
        }

        public void put(int key, AllCampaignTypeData data) {
            dataMap.put(key, data);
        }
    }

    private static class UserSegmentTree {
        // key : year
        Map<Integer, SegmentNodeData> treeMap = new ConcurrentHashMap<>();

        public SegmentNodeData get(int key) {
            return treeMap.get(key);
        }

        public void put(int key, SegmentNodeData data) {
            treeMap.put(key, data);
        }

        public boolean containsKey(int year) {
            return treeMap.containsKey(year);
        }
    }

    // key : email
    private final Map<String, UserSegmentTree> lazyCacheTree = new ConcurrentHashMap<>();

    public Long getCacheHitCount(){
        return hitCount.sum();
    }

    private SegmentNodeData getSegmentNodeDataSafe(String email, int year) {
        return lazyCacheTree
                .computeIfAbsent(email, k -> new UserSegmentTree()) // 유저 없으면 생성
                .treeMap
                .computeIfAbsent(year, k -> new SegmentNodeData()); // 연도 없으면 생성
    }

    public int makeKey(int start, int end) {
        return (start << 9) | end;
    }

    public int convertLocalDateToCount(LocalDate localDate) {
        LocalDate startOfYear = LocalDate.of(localDate.getYear(), 1, 1);
        return (int) ChronoUnit.DAYS.between(startOfYear, localDate) + 1;
    }

    public LocalDate convertCountToLocalDate(int year, int count) {
        return LocalDate.of(year, 1, 1).plusDays(count - 1);
    }

    public AllCampaignTypeData find(String email, int year, int nodeStart, int nodeEnd, int targetStart, int targetEnd) {
        if (targetEnd < nodeStart || targetStart > nodeEnd) return new AllCampaignTypeData();
        if (targetStart <= nodeStart && nodeEnd <= targetEnd) {
            return getOrLoadNode(email,year, nodeStart, nodeEnd);
        }
        int mid = (nodeStart + nodeEnd) / 2;
        return find(email,year, nodeStart, mid, targetStart, targetEnd).sum(find(email,year, mid + 1, nodeEnd, targetStart, targetEnd));
    }



    private AllCampaignTypeData getOrLoadNode(String email, int year, int start, int end) {
        requestCount.increment();
        int key = makeKey(start, end);
        SegmentNodeData segmentNodeData = getSegmentNodeDataSafe(email,year);
        if (segmentNodeData.containsKey(key)) {
            hitCount.increment();
            log.debug("캐싱 hit! key : " + key);
            return segmentNodeData.getCampaignAnalysisDto(key);
        }
        LocalDate startDate = convertCountToLocalDate(year, start);
        LocalDate endDate = convertCountToLocalDate(year, end);
        List<Tuple> dbResult = keywordService.getAllTypeOfCampaignAdCostSumAndAdSaleSumByPeriodAndEmailByList(email,startDate,endDate);
        Map<Integer, AllCampaignTypeData> dailyDataMap = mapDbResultToDailyData(dbResult);        // C. 이제 안전하게 저장!
        fillLeafNodes(segmentNodeData, dailyDataMap, start, end);
        return buildTree(segmentNodeData,start,end);
    }

    private void fillLeafNodes(SegmentNodeData segmentNodeData, Map<Integer, AllCampaignTypeData> dailyDataMap, int start, int end) {
        for (int i = start; i <= end; i++) {
            int leafKey = makeKey(i, i);
            AllCampaignTypeData data = dailyDataMap.getOrDefault(leafKey, new AllCampaignTypeData());
            segmentNodeData.put(leafKey, data);
        }
    }

    private AllCampaignTypeData buildTree(SegmentNodeData segmentNodeData, int startCount, int endCount){
        if(startCount == endCount) {
            int key = makeKey(startCount,endCount);
            return segmentNodeData.getCampaignAnalysisDto(key);
        }

        int mid = (startCount + endCount)/2;
        AllCampaignTypeData leftValue = buildTree(segmentNodeData,startCount,mid);
        AllCampaignTypeData rightValue = buildTree(segmentNodeData,mid+1,endCount);

        int key = makeKey(startCount,endCount);
        AllCampaignTypeData sumValue = leftValue.add(rightValue);
        segmentNodeData.put(key,sumValue);
        return sumValue;
    }

    private  Map<Integer, AllCampaignTypeData> mapDbResultToDailyData(List<Tuple> dbResult){
        Map<Integer, AllCampaignTypeData> map = new HashMap<>();

        for (Tuple tuple : dbResult) {
            LocalDate date = tuple.get(keyword.date);

            int count = convertLocalDateToCount(date);
            int key = makeKey(count,count);

            String type = tuple.get(campaign.camAdType);
            Double cost = tuple.get(keyword.adCost.sum());
            Double sales = tuple.get(keyword.adSales.sum());
            double safeCost = (cost != null) ? cost : 0.0;
            double safeSales = (sales != null) ? sales : 0.0;

            AllCampaignTypeData newData = new AllCampaignTypeData(type, safeCost, safeSales);
            map.merge(key, newData, AllCampaignTypeData::add);
        }
        return map;
    }

    public Map<String, Long> getCacheStats() {
        return Map.of(
                "total", requestCount.sum(),
                "hit", hitCount.sum(),
                "miss", requestCount.sum() - hitCount.sum()
        );
    }

    public void resetCacheStats() {
        requestCount.reset();
        hitCount.reset();
    }

    public void incrementRequestCount(){
        requestCount.increment();;
    }
}