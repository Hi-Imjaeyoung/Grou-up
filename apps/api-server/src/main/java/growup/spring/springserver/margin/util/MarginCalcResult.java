package growup.spring.springserver.margin.util;

public record MarginCalcResult(
        long actualSales,
        long adMargin,
        long returnCount,
        double returnCost
) {
    /*
        0으로 초기화된 MarginCalcResult 객체를 생성
    */
    public static MarginCalcResult createZero() {
        return new MarginCalcResult(0L, 0L, 0L, 0.0);
    }
    /*
        기존 MarginCalcResult 객체에 다른 MarginCalcResult 객체의 값을 더하여 새로운 MarginCalcResult 객체를 반환
    */
    public MarginCalcResult add(MarginCalcResult other) {
        return new MarginCalcResult(
                this.actualSales + other.actualSales,
                this.adMargin + other.adMargin,
                this.returnCount + other.returnCount,
                this.returnCost + other.returnCost
        );
    }
}