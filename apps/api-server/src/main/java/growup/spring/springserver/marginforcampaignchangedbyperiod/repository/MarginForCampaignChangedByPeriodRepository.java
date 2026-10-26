package growup.spring.springserver.marginforcampaignchangedbyperiod.repository;

import growup.spring.springserver.marginforcampaignchangedbyperiod.domain.MarginForCampaignChangedByPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MarginForCampaignChangedByPeriodRepository extends JpaRepository<MarginForCampaignChangedByPeriod, Long> {

    /*
        특정 MarginForCampaign ID와 날짜 범위에 해당하는 MarginForCampaignChangedByPeriod 엔티티들을 조회하는 메서드
     */
    @Query("""
            
                SELECT mcp
            FROM MarginForCampaignChangedByPeriod mcp
            WHERE mcp.marginForCampaign.id = :mfcId
              AND mcp.date BETWEEN :start AND :end
            """)
    List<MarginForCampaignChangedByPeriod> findAllByMarginForCampaign_IdAndDateRange(
            @Param("mfcId") Long mfcId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    /*
        여러 MarginForCampaign ID들과 날짜 범위에 해당하는 MarginForCampaignChangedByPeriod 엔티티들을 조회하는 메서드
    */
    @Query("""
            SELECT mcpcbp
            FROM MarginForCampaignChangedByPeriod mcpcbp
            JOIN FETCH mcpcbp.marginForCampaign mfc
            WHERE mfc.id IN :mfcIds 
              AND mcpcbp.date BETWEEN :start AND :end
            """)
    List<MarginForCampaignChangedByPeriod> findAllByMfcIdsAndDateRange(
            @Param("mfcIds") List<Long> mfcIds,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MarginForCampaignChangedByPeriod m WHERE m.marginForCampaign.id = :mfcId")
    void deleteByMfcId(@Param("mfcId") Long mfcId);
}
