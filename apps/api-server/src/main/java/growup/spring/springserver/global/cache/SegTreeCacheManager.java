package growup.spring.springserver.global.cache;

import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SegTreeCacheManager {

    private final Map<String, Map<String, AllCampaignTypeData>> cacheTree = new HashMap<>();
    private final Map<String, Set<String>> leafNodeKeys = new HashMap<>();
    private final Map<String, String> rootKeyMap = new HashMap<>();

    public String getCacheTree() {
        StringBuilder sb = new StringBuilder();
        for (String key : cacheTree.keySet()) {
            sb.append("{").append(key).append(" = ").append("\n");
            Map<String, AllCampaignTypeData> afterMap = cacheTree.get(key);
            for (String key2 : afterMap.keySet()) {
                sb.append("    {").append(key2).append(" : ").append(afterMap.get(key2).getCampaignAnalysisDtoMap().toString()).append("}");
                sb.append("\n");
            }
            sb.append("}").append("\n");
        }
        return sb.toString();
    }

    public void init(String email) {
        cacheTree.put(email, new HashMap<>());
        leafNodeKeys.put(email, new HashSet<>()); // 중복 방지를 위해 Set 사용
    }

    public void addLeafNode(String email, LocalDate date, String type, double adCost, double adSales) {
        String key = makeKeySet(email, date, date); // 시작일:종료일
        // Map.merge: 키가 없으면 새거 넣고, 있으면 기존거에 합쳐라 (핵심!)
        cacheTree.get(email).merge(key,
                new AllCampaignTypeData(type, adCost, adSales),
                (existing, newOne) -> existing.add(newOne) // DTO 내부에 add 메서드 구현 필요
        );
        leafNodeKeys.get(email).add(key);
    }

    // 2. 트리 빌드 (Bottom-Up)
    public void buildTree(String email) {
        // 날짜 순서대로 정렬 (필수! 뒤죽박죽이면 범위가 꼬임)
        List<String> currentLevelKeys = leafNodeKeys.get(email).stream()
                .sorted()
                .collect(Collectors.toList());
        // 층을 하나씩 위로 쌓아 올림
        while (currentLevelKeys.size() > 1) {
            List<String> nextLevelKeys = new ArrayList<>();
            // 두 개씩 묶어서 부모 만들기
            for (int i = 0; i < currentLevelKeys.size(); i += 2) {
                // 짝이 없으면(마지막 하나 남음) 그냥 위로 올림
                if (i + 1 >= currentLevelKeys.size()) {
                    nextLevelKeys.add(currentLevelKeys.get(i));
                    break;
                }
                String leftKey = currentLevelKeys.get(i);
                String rightKey = currentLevelKeys.get(i + 1);
                AllCampaignTypeData leftData = cacheTree.get(email).get(leftKey);
                AllCampaignTypeData rightData = cacheTree.get(email).get(rightKey);
                // 부모 키 생성 (Left 시작일 : Right 종료일)
                String parentKey = makeParentKey(leftKey, rightKey);
                // 부모 데이터 생성 (합치기)
                AllCampaignTypeData parentData = sumValue(leftData, rightData);
                // 맵에 저장 & 다음 층 리스트에 등록
                cacheTree.get(email).put(parentKey, parentData);
                nextLevelKeys.add(parentKey);
            }
            // 한 층 다 쌓았으면, 기준을 위층으로 변경
            currentLevelKeys = nextLevelKeys;
        }
        // 메모리 절약을 위해 임시 Set 비우기
        leafNodeKeys.get(email).clear();
    }

    // 루트 키 가져오기 (전체 범위)
    public String getRootNodeKey(String email) {
        return rootKeyMap.get(email);
    }

    // 키 생성 (email:start:end)
    private String makeKeySet(String email, LocalDate start, LocalDate end) {
        return email + ":" + start.toString() + ":" + end.toString();
    }

    // 부모 키 생성 로직 (문자열 파싱)
    private String makeParentKey(String key1, String key2) {
        // key format: email:start:end
        String[] part1 = key1.split(":");
        String[] part2 = key2.split(":");
        // parent: email:key1_start:key2_end
        return part1[0] + ":" + part1[1] + ":" + part2[2];
    }

    private AllCampaignTypeData sumValue(AllCampaignTypeData leftData, AllCampaignTypeData rightData) {
        // 1. 빈 깡통 객체(부모)를 만듦
        AllCampaignTypeData parentData = new AllCampaignTypeData();
        // 2. 왼쪽 자식의 모든 타입 데이터를 부모에게 들이부음
        parentData.add(leftData);
        // 3. 오른쪽 자식의 모든 타입 데이터를 부모에게 들이부음 (중복된 타입은 알아서 합쳐짐)
        parentData.add(rightData);
        return parentData;
    }

    public void setRootKey(String email, LocalDate start, LocalDate end) {
        rootKeyMap.put(email, makeKeySet(email, start, end));
    }

    public AllCampaignTypeData checkPeriod(String email, LocalDate start, LocalDate end) {
        // 1. rootKey를 갖고오기
        String rootKey = getRootNodeKey(email);
        // 2. rootkey와 요청 값 비교
        if (comparePeriod(rootKey, start, end)) {
            // 2-1. rootKey에 속해있다.
            // data 갖고 오기
            String[] rootKerArr = rootKey.split(":");
            LocalDate rootStart = LocalDate.parse(rootKerArr[1], DateTimeFormatter.ISO_DATE);
            LocalDate rootEnd = LocalDate.parse(rootKerArr[2], DateTimeFormatter.ISO_DATE);
            log.debug("캐싱 hit");
            return getDataInTree(email, rootStart, rootEnd, start, end);
        } else {
            // 2-2. rootKey를 벗어난다.
            log.debug("캐시 Hit가 이뤄지지 않았습니다.");
        }
        return null;
    }

    public AllCampaignTypeData getDataInTree(String email,
                                             LocalDate rootStart,
                                             LocalDate rootEnd,
                                             LocalDate start,
                                             LocalDate end) {
        log.debug("root start : " +rootStart);
        log.debug("root end : " +rootEnd);
        if (end.isBefore(rootStart) || start.isAfter(rootEnd)) {
            return new AllCampaignTypeData();
        }
        // 2. [완전 포함] 현재 노드의 범위가 조회 범위 안에 쏙 들어오면? -> 캐시(노드) 통째로 리턴! (★ 핵심)
        if (start.isBefore(rootStart.plusDays(1)) && end.isAfter(rootEnd.minusDays(1))) {
            // 즉, queryStart <= nodeStart && nodeEnd <= queryEnd
            String key = makeKeySet(email, rootStart, rootEnd);
            if (cacheTree.get(email).containsKey(key)) {
                // 여기가 바로 "뭉텅이"로 캐시 히트하는 지점!
                log.debug("캐싱 히트! :" + rootStart +" " + rootEnd );
                return cacheTree.get(email).get(key);
            }
        }
        if (rootStart.equals(rootEnd)) {
            // 캐시에도 없고, 더 쪼갤 수도 없다? -> 데이터 없음(또는 DB 조회)
            return new AllCampaignTypeData();
        }
        // 중간값 구하기.
        LocalDate mid = calculateMiddleDate(rootStart, rootEnd);
        AllCampaignTypeData leftData = getDataInTree(email,rootStart,mid,start, end);
        AllCampaignTypeData rightData = getDataInTree(email,mid.plusDays(1),rootEnd,start, end);
        return sumAllCampaignTypeData(leftData, rightData);
    }

    private AllCampaignTypeData sumAllCampaignTypeData(AllCampaignTypeData data1, AllCampaignTypeData data2) {
        return data1.add(data2);
    }

    private void saveResultInTree(String email, String key, AllCampaignTypeData result) {
        cacheTree.get(email).put(key, result);
    }

    private LocalDate calculateMiddleDate(LocalDate start, LocalDate end) {
        long startDay = start.toEpochDay();
        long endDay = end.toEpochDay();
        long midDay = (startDay + endDay) / 2;
        return LocalDate.ofEpochDay(midDay);
    }

    private boolean comparePeriod(String rootKey, LocalDate start, LocalDate end) {
        if (rootKey == null) {
            log.debug("rootKey가 누락이라 에러를 발생");
            throw new GrouException(ErrorCode.EXECUTION_REQUEST_ERROR);
        }
        String[] keyData = rootKey.split(":");
        LocalDate rootStart = LocalDate.parse(keyData[1]);
        LocalDate rootEnd = LocalDate.parse(keyData[2]);
        if (rootStart.isAfter(start) || rootEnd.isBefore(end))
            throw new GrouException(ErrorCode.EXECUTION_REQUEST_ERROR);
        return true;
    }
}