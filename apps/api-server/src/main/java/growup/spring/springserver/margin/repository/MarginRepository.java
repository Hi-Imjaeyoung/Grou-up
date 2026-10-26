package growup.spring.springserver.margin.repository;

import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.margin.dto.DailyAdSummaryDto;
import growup.spring.springserver.margin.dto.DailyNetProfitResponseDto;
import growup.spring.springserver.margin.dto.MarginOverviewResponseDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Repository
public interface MarginRepository extends JpaRepository<Margin, Long> {

    @Query("SELECT m FROM Margin m WHERE m.campaign.campaignId IN :campaignIds AND m.marDate BETWEEN :startDate AND :endDate")
    List<Margin> findByCampaignIdsAndDates(
            @Param("campaignIds") List<Long> campaignIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT new growup.spring.springserver.margin.dto.DailyAdSummaryDto(" +
            "m.marDate, " +
            "CAST(COALESCE(SUM(m.marSales), 0) AS double), " +
            "CAST(COALESCE(SUM(m.marNetProfit), 0) AS double))" +
            "FROM Margin m " +
            "WHERE m.campaign.campaignId IN :campaignIds " +
            "AND m.marDate BETWEEN :startDate AND :endDate " +
            "GROUP BY m.marDate " +
            "ORDER BY m.marDate")
    List<DailyAdSummaryDto> findMarginOverviewGraphByCampaignIdsAndDate(
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
            "m.marDate, SUM(m.marNetProfit),SUM(m.marReturnCost),sum(m.marReturnCount),sum(m.marSales)) " +
            "FROM Margin m " +
            "WHERE m.campaign.member.email = :email " +  // 내 캠페인만 필터링
            "AND m.marDate BETWEEN :start AND :end " +
            "GROUP BY m.marDate " +
            "ORDER BY m.marDate")
    List<DailyNetProfitResponseDto> findTotalMarginByDateRangeAndEmail(@Param("start") LocalDate start,
                                                                       @Param("end") LocalDate end,
                                                                       @Param("email") String email);

    @Modifying
    @Query("DELETE FROM Margin m WHERE m.marDate BETWEEN :start AND :end " +
            "AND m.campaign.campaignId IN:campaignIds")
    int deleteByCampaignIdAndDate(@Param("start") LocalDate start,
                                  @Param("end") LocalDate end,
                                  @Param("campaignIds") List<Long> campaignIds);

    @Modifying
    @Query("DELETE FROM Margin m WHERE m.campaign.campaignId IN:campaignIds")
    int deleteByCampaignId(@Param("campaignIds") List<Long> campaignIds);

    @Query("SELECT MAX(m.marDate) FROM Margin m WHERE m.campaign.member.email = :email")
    Optional<LocalDate> findLatestMarginDateByEmail(@Param("email") String email);

    List<Margin> findAllByCampaignCampaignIdInAndMarDate(List<Long> campaignIds, LocalDate marDate);

    @Query("SELECT new growup.spring.springserver.margin.dto.MarginOverviewResponseDto(" +
            "   m.campaign.campaignId, " +
            "   m.campaign.camCampaignName, " +
            // 모든 COALESCE 결과를 CAST AS double로 감싸서 타입을 명시
            "   CAST(COALESCE(SUM(m.marSales), 0.0) AS double), " +
            "   CAST(COALESCE(SUM(m.marNetProfit), 0.0) AS double), " +
            // 마진율
            "   CAST(CASE WHEN COALESCE(SUM(m.marSales), 0.0) = 0 THEN 0.0 " +
            "        ELSE ROUND((COALESCE(SUM(m.marNetProfit), 0.0) / COALESCE(SUM(m.marSales), 0.0)) * 100, 2) END AS double), " +
            // ROI
            "   CAST(CASE WHEN COALESCE(SUM(m.marAdCost), 0.0) = 0 THEN 0.0 " +
            "        ELSE ROUND((COALESCE(SUM(m.marNetProfit), 0.0) / COALESCE(SUM(m.marAdCost), 0.0)) * 100, 2) END AS double), " +
            "   CAST(COALESCE(SUM(m.marAdCost), 0.0) AS double), " +
            "   CAST(COALESCE(SUM(m.marReturnCount), 0.0) AS long), " +
            "   CAST(COALESCE(SUM(m.marReturnCost), 0.0) AS double), " +
            "   CAST(COALESCE(SUM(m.marAdConversionSalesCount), 0.0) AS long), " +
            // 반품률
            "   CAST(CASE WHEN COALESCE(SUM(m.marAdConversionSalesCount), 0.0) = 0 THEN 0.0 " +
            "        ELSE ROUND((COALESCE(SUM(m.marReturnCount), 0.0) * 1.0 / COALESCE(SUM(m.marAdConversionSalesCount), 0.0)) * 100, 2) END AS double) " +
            ") " +
            "FROM Margin m " +
            "WHERE m.campaign.campaignId IN :campaignIds " +
            "AND m.marDate BETWEEN :startDate AND :endDate " +
            "GROUP BY m.campaign.campaignId, m.campaign.camCampaignName " +
            "ORDER BY SUM(m.marSales) DESC")
    List<MarginOverviewResponseDto> findMarginOverviewByCampaignIdsAndDate(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("campaignIds") List<Long> campaignIds
    );

    @Query("SELECT DISTINCT m.marDate FROM Margin m WHERE m.campaign.member.email = :email AND m.marDate BETWEEN :startDate AND :endDate")
    List<LocalDate> findDistinctAdvDatesByEmailAndDateBetween(
            @Param("email") String email,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    @Query("""
        SELECT DISTINCT m.marDate
        FROM Margin m
        WHERE m.campaign.campaignId = :campaignId
          AND m.marDate IN :dates
        """)
    Set<LocalDate> findExistingDatesByCampaignIdAndDateIn(
            @Param("campaignId") Long campaignId,
            @Param("dates") List<LocalDate> dates);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Margin m " +
            "SET m.marAdMargin = 0," +
            " m.marNetProfit = 0 ," +
            " m.marActualSales=0," +
            " m.marReturnCost=0," +
            " m.marReturnCount=0," +
            " m.marUpdated = true " +
            "WHERE m.marDate = :date AND m.campaign.campaignId IN :campaignIds")
    int resetNetSalesInfo(@Param("date") LocalDate date, @Param("campaignIds") List<Long> campaignIds);
}
