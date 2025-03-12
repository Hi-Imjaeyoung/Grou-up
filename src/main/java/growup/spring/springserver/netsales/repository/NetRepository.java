package growup.spring.springserver.netsales.repository;

import growup.spring.springserver.marginforcampaign.support.MarginType;
import growup.spring.springserver.netsales.domain.NetSales;
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
}