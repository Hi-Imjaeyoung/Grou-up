package growup.spring.springserver.netsales.repository;

import growup.spring.springserver.marginforcampaign.support.MarginType;
import growup.spring.springserver.netsales.domain.NetSales;
import growup.spring.springserver.netsales.dto.DailySalesDto;
import growup.spring.springserver.netsales.dto.NetSalesSummaryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NetRepository extends JpaRepository<NetSales, Long> {

    @Query("SELECT ns FROM NetSales ns WHERE ns.netDate = :netDate " +
            "AND ns.member.email = :email " +
            "AND ns.netProductName = :netProductName " +
            "AND ns.netType = :netType")
    Optional<NetSales> findByNetDateAndEmailAndNetProductNameAndNetMarginType(
            @Param("netDate") LocalDate netDate,
            @Param("email") String email,
            @Param("netProductName") String netProductName,
            @Param("netType") MarginType netType);

    @Query("SELECT DISTINCT ns.netDate FROM NetSales ns WHERE ns.member.email = :email AND ns.netDate BETWEEN :startDate AND :endDate")
    List<LocalDate> findDatesWithNetSalesByEmailAndDateRange(
            @Param("email") String email,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    int deleteByMemberEmailAndNetDate(String email, LocalDate netDate);

    @Query("""
    SELECT new growup.spring.springserver.netsales.dto.DailySalesDto(
        ns.netDate,
        COALESCE(SUM(ns.netCancelPrice), 0.0),
        COALESCE(SUM(ns.netSalesAmount), 0.0)
    )
    FROM NetSales ns
    WHERE ns.member.email = :email
      AND ns.netDate BETWEEN :start AND :end
    GROUP BY ns.netDate
    ORDER BY ns.netDate ASC
    """)
    List<DailySalesDto> findDailySalesByEmail(
            @Param("email") String email,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // 기간 조회 -> Map 형태로 변환 시킬 예정
//    @Query("""
//    SELECT ns
//    FROM NetSales ns
//    JOIN FETCH ns.member m
//    WHERE m.email = :email
//      AND ns.netDate BETWEEN :start AND :end
//""")
    @Query("SELECT ns FROM NetSales ns WHERE ns.member.email = :email AND ns.netDate BETWEEN :start AND :end")
    List<NetSales> findAllByEmailAndDateRange(
            @Param("email") String email,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
    @Query("""
        SELECT new growup.spring.springserver.netsales.dto.NetSalesSummaryDto(
            ns.netDate,
            ns.netProductName,
            ns.netType,
            SUM(ns.netSalesAmount),
            SUM(ns.netSalesCount),
            SUM(ns.netReturnCount),
            SUM(ns.netCancelPrice)
        )
        FROM NetSales ns
        WHERE ns.member.email = :email
          AND ns.netDate BETWEEN :start AND :end
        GROUP BY ns.netDate, ns.netProductName, ns.netType
    """)
    List<NetSalesSummaryDto> findSummaryByEmailAndDateRange(
            @Param("email") String email,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}