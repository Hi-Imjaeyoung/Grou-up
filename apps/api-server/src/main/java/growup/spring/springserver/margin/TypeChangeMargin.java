package growup.spring.springserver.margin;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.dto.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TypeChangeMargin {

    /**
     *
     * @param campaign        해당 캠페인
     * @param todayMargin     오늘의 Margin 데이터
     * @param yesterdayMargin 어제의 Margin 데이터
     * @return MarginSummaryResponseDto로 변환된 결과
     */
    public static MarginSummaryResponseDto entityToMarginSummaryResponseDto(
            Campaign campaign,
            Margin todayMargin,
            Margin yesterdayMargin) {

        // 오늘과 어제의 매출 계산
        double todaySales = todayMargin.getMarSales() != null ? todayMargin.getMarSales() : 0.0;
        double yesterdaySales = yesterdayMargin.getMarSales() != null ? yesterdayMargin.getMarSales() : 0.0;
        double differentSales = todaySales - yesterdaySales;

        // DTO 생성 및 반환
        return MarginSummaryResponseDto.builder()
                .date(todayMargin.getMarDate()) // 날짜
                .campaignId(campaign.getCampaignId()) // 캠페인 ID
                .campaignName(campaign.getCamCampaignName()) // 캠페인 이름
                .todaySales(todaySales) // 오늘 매출
                .yesterdaySales(yesterdaySales) // 어제 매출
                .differentSales(differentSales) // 매출 차이
                .build();
    }

    public static Margin createDefaultMargin(Campaign campaign, LocalDate date) {
        return Margin.builder()
                .campaign(campaign)
                .marDate(date)
                .marSales(0.0) // 기본값 설정
                .build();
    }
    public static Margin createSaveDefaultMargin(Campaign campaign, LocalDate date) {
        return Margin.builder()
                .campaign(campaign)
                .marDate(date)
                .marImpressions(0L)
                .marClicks(0L)
                .marAdConversionSales(0L)
                .marAdConversionSalesCount(0L)
                .marSales(0.0)
                .marTargetEfficiency(0.0)
                .marAdBudget(0.0)
                .marAdMargin(0L)
                .marNetProfit(0.0)
                .marActualSales(0L)
                .marAdCost(0.0)
                .marReturnCount(0L)
                .marReturnCost(0.0)
                .build();
    }
    public static MarginUpdateResponseDto marginValidationResponse(int responseNumber, int requestNumber, Map<LocalDate, Map<String, Double>> failData) {
        return MarginUpdateResponseDto.builder()
                .requestNumber(requestNumber)
                .responseNumber(responseNumber)
                .failedDate(failData)
                .build();
    }

    public static DailyMarginSummary getDailyMarginSummary(Margin marginData, String productName) {

        return DailyMarginSummary.builder()
                .marProductName(productName)
                .marAdMargin(marginData.getMarAdMargin())
                .marNetProfit(marginData.getMarNetProfit())
                .build();
    }
    public static MarginResponseDto createMarginResponseDto(Long campaignId, List<MarginResultDto> data) {
        return MarginResponseDto.builder()
                .campaignId(campaignId)
                .data(data)
                .build();
    }
    public static MarginOverviewResponseDto createOthersSummary(List<MarginOverviewResponseDto> etcDto) {

        // type 바꾸는 곳 인데, 마진계산, 이동이 필요하다.
        // 1. 모든 합계 계산 (for-each 사용, 단 1회 순회)
        double othersTotalSales = 0;
        double othersTotalAdCost = 0;
        double othersTotalNetProfit = 0;
        double othersTotalReturnCost = 0;
        long othersTotalReturnCount = 0;
        long othersTotalAdConversionSalesCount = 0;

        for (MarginOverviewResponseDto dto : etcDto) {
            othersTotalSales += dto.getMarSales();
            othersTotalAdCost += dto.getMarAdCost();
            othersTotalNetProfit += dto.getMarNetProfit();
            othersTotalReturnCost += dto.getMarReturnCost();
            othersTotalReturnCount += dto.getMarReturnCount();
            othersTotalAdConversionSalesCount += dto.getMarAdConversionSalesCount();
        }

// 2. 합산된 값으로 비율 계산 (기존과 동일)
        double othersTotalMarginRate = (othersTotalSales == 0) ? 0 : Math.round((othersTotalNetProfit / othersTotalSales) * 10000.0) / 100.0;
        double othersTotalROI = (othersTotalAdCost == 0) ? 0 : Math.round((othersTotalNetProfit / othersTotalAdCost) * 10000.0) / 100.0;
        double otherReturnsRate = (othersTotalAdConversionSalesCount == 0) ? 0 : Math.round(((double) othersTotalReturnCount / othersTotalAdConversionSalesCount) * 10000.0) / 100.0;

        return MarginOverviewResponseDto.builder()
                .campaignId(0L)
                .campaignName("기타")
                .marSales(othersTotalSales)
                .marNetProfit(othersTotalNetProfit)
                .marMarginRate(othersTotalMarginRate)
                .marRoi(othersTotalROI)
                .marAdCost(othersTotalAdCost)
                .marReturnCount(othersTotalReturnCount)
                .marAdConversionSalesCount(othersTotalAdConversionSalesCount)
                .marReturnCost(othersTotalReturnCost)
                .marReturnRate(otherReturnsRate)
                .build();

    }
}