package growup.spring.springserver.margin.repository;

import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.dto.DailyAdSummaryDto;
import growup.spring.springserver.margin.dto.DailyNetProfitResponseDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Repository
public interface MarginRepository extends JpaRepository<Margin, Long> {

    @Query("SELECT m FROM Margin m WHERE m.campaign.campaignId IN :campaignIds AND m.marDate BETWEEN :startDate AND :endDate")
    List<Margin> findByCampaignIdsAndDates(
            @Param("campaignIds") List<Long> campaignIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT new growup.spring.springserver.margin.dto.DailyAdSummaryDto(" +
            "m.marDate, " +
            "CAST(COALESCE(SUM(m.marAdCost), 0) AS double), " + // 광고비 합계를 Double로 변환
            "CAST(COALESCE(SUM(m.marSales), 0) AS double), " + // 매출 합계를 Double로 변환
            "CAST(CASE WHEN COALESCE(SUM(m.marAdCost), 0) = 0 THEN 0.0 " +
            "ELSE ROUND((COALESCE(SUM(m.marSales), 0) / COALESCE(SUM(m.marAdCost), 0)) * 10000) / 100.0 END AS double)" + // ROAS를 Double로 변환
            ") " +
            "FROM Margin m " +
            "WHERE m.campaign.campaignId IN :campaignIds " +
            "AND m.marDate BETWEEN :startDate AND :endDate " +
            "GROUP BY m.marDate " +
            "ORDER BY m.marDate")
    List<DailyAdSummaryDto> find7daysTotalsByCampaignIds(
            @Param("campaignIds") List<Long> campaignIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
// COALESCE(a,0) 첫번째 인자 null 이면 뒤에꺼 반환
//광고비 500원 판매된게 200원
//    = marsales / maradcost

    @Query("SELECT m FROM Margin m WHERE m.campaign.campaignId = :campaignId AND m.marDate BETWEEN :startDate AND :endDate")
    List<Margin> findByCampaignIdAndDates(@Param("campaignId") Long campaignId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT m from Margin m where m.campaign.campaignId = :campaignId AND m.marDate = :date")
    Optional<Margin> findByCampaignIdAndDate(Long campaignId, LocalDate date);

    @Query("SELECT new growup.spring.springserver.margin.dto.DailyNetProfitResponseDto(" +
            "m.marDate, SUM(m.marNetProfit),SUM(m.marReturnCost)) " +
            "FROM Margin m " +
            "WHERE m.campaign.member.email = :email " +  // 내 캠페인만 필터링
            "AND m.marDate BETWEEN :start AND :end " +
            "GROUP BY m.marDate " +
            "ORDER BY m.marDate")
    List<DailyNetProfitResponseDto> findTotalMarginByDateRangeAndEmail(@Param("start") LocalDate start,
                                                                       @Param("end") LocalDate end,
                                                                       @Param("email") String email);


}
