package growup.spring.springserver.exclusionKeyword.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.exclusionKeyword.domain.ExclusionKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExclusionKeywordRepository extends JpaRepository<ExclusionKeyword,Long> {
    List<ExclusionKeyword> findAllByCampaign_campaignId(Long campaignId);

    @Query("SELECT count(e) > 0 FROM ExclusionKeyword e WHERE e.exclusionKeyword = :keyword AND e.campaign.campaignId = :campaignId")
    boolean existsByExclusionKeyword(@Param("keyword") String keyword, @Param("campaignId") Long campaignId);


    @Modifying
    @Query("DELETE FROM ExclusionKeyword k WHERE k.campaign.campaignId = :campaignId AND k.exclusionKeyword = :exclusionKeyword")
    int deleteByCampaign_campaignIdANDExclusionKeyword(@Param("campaignId") Long campaignId,
                                                        @Param("exclusionKeyword") String exclusionKeyword);
    @Modifying
    @Query("DELETE FROM ExclusionKeyword k WHERE k.campaign.campaignId IN :campaignIds")
    int deleteAllByCampaignIds(@Param("campaignIds") List<Long> campaignIds);
}
