package growup.spring.springserver.memo.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.memo.domain.Memo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MemoRepository extends JpaRepository<Memo,Long> {
    List<Memo> findAllByCampaign(Campaign campaign);
    @Modifying
    @Query("DELETE FROM Memo m WHERE m.id = :id")
    int deleteMemoById(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM Memo m WHERE m.date BETWEEN :start AND :end " +
            "AND m.campaign.campaignId IN :campaignIds")
    int deleteByCampaignIdAndDate(@Param("start") LocalDate start,
                                  @Param("end") LocalDate end,
                                  @Param("campaignIds") List<Long> campaignIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Memo m WHERE m.campaign.campaignId IN :campaignIds")
    int deleteAllByCampaignIds(@Param("campaignIds") List<Long> campaignIds);

    @Query("SELECT m FROM Memo m WHERE m.date BETWEEN :start AND :end " +
            "AND m.campaign.campaignId = :campaignId")
    List<Memo> findByDateAndCampaignId(@Param("start") LocalDate start,
                                       @Param("end") LocalDate end,
                                       @Param("campaignId") Long campaignId);
}
