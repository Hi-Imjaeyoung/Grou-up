package growup.spring.springserver.databaseutils.testdatagenerator;

import growup.spring.springserver.global.domain.CoupangExcelData;
import growup.spring.springserver.keyword.domain.Keyword;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class CoupagRandomData extends CoupangExcelData {
    static final String KEYWORD = "keyword";
    static final String OPTION = "option";
    static Random random = new Random();
    public static CoupangExcelData createRandomData(){
        CoupangExcelData coupangExcelData = CoupangExcelData.builder()
                .impressions(random.nextLong(100000)) // 0 ~ 99,999 사이의 Long 값
                .clicks(random.nextLong(5000))      // 0 ~ 4,999 사이의 Long 값
                .totalSales(random.nextLong(100))    // 0 ~ 99 사이의 Long 값
                .adCost((double)random.nextLong(1000000)) // 0.0 ~ 1,000,000.0 사이의 Double 값
                .adSales((double)random.nextLong(5000000)) // 0.0 ~ 5,000,000.0 사이의 Double 값
                .build();
        coupangExcelData.calculatePercentData();
        return coupangExcelData;
    }

    public static LocalDate between(LocalDate startInclusive, LocalDate endExclusive) {
        // 1. 시작 날짜와 종료 날짜를 epoch day(long)로 변환
        long startEpochDay = startInclusive.toEpochDay();
        long endEpochDay = endExclusive.toEpochDay();
        // 2. 두 epoch day 사이의 랜덤한 long 값을 생성
        long randomDay = ThreadLocalRandom
                .current()
                .nextLong(startEpochDay, endEpochDay);
        // 3. 랜덤하게 생성된 epoch day를 다시 LocalDate로 변환하여 반환
        return LocalDate.ofEpochDay(randomDay);
    }

    public static String createRandomKeyword(){
        return KEYWORD + random.nextLong(100);
    }

    public static Map<String,Long> createKeyProductSales(Long totalSales){
        Map<String,Long> map = new HashMap<>();
        map.put(OPTION + random.nextLong(10),totalSales);
        return map;
    }

}
