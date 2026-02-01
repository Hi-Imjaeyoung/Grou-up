package growup.spring.springserver.keyword.repository;

import growup.spring.springserver.keyword.domain.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface KeywordRepository extends JpaRepository <Keyword,Long>, KeywordRepositoryCustom{

    //TODO: 쿼리 순서 check!
    @Query("SELECT k FROM Keyword k WHERE k.date BETWEEN :startDate AND :endDate " +
            "AND k.campaign.campaignId = :campaignId")
    List<Keyword> findAllByDateANDCampaign(@Param("startDate") LocalDate startDate,
                                           @Param("endDate")LocalDate endDate,
                                           @Param("campaignId")Long campaignId);

    @Query("SELECT k FROM Keyword k WHERE k.date BETWEEN :start AND :end " +
            "AND k.campaign.campaignId = :campaignId " +
            "AND k.keyKeyword IN :keys")
    List<Keyword> findKeywordsByDateAndCampaignIdAndKeys(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("campaignId") Long campaignId,
            @Param("keys") List<String> keys);


    @Modifying
    @Query("DELETE FROM Keyword k WHERE k.date BETWEEN :start AND :end " +
            "AND k.campaign.campaignId IN :campaignIds")
    int deleteByCampaignIdAndDate(@Param("start") LocalDate start,
                                  @Param("end") LocalDate end,
                                  @Param("campaignIds") List<Long> campaignIds);

    @Modifying(clearAutomatically = true,flushAutomatically = true)
    @Query("DELETE FROM Keyword k WHERE k.campaign.campaignId IN :campaignIds")
    int deleteAllByCampaignIds(@Param("campaignIds") List<Long> campaignIds);
}
