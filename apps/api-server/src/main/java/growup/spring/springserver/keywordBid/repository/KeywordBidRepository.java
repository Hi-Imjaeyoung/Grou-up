package growup.spring.springserver.keywordBid.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.keywordBid.domain.KeywordBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KeywordBidRepository extends JpaRepository<KeywordBid,Long> {

    @Query("SELECT k FROM KeywordBid k WHERE k.keyword = :keyword " +
            "AND k.campaign.campaignId = :campaignId")
    Optional<KeywordBid> findByCampaign_CampaignIdANDKeyword(@Param("campaignId") Long campaignId,
                                                             @Param("keyword") String keyword);
    @Modifying
    @Query("DELETE FROM KeywordBid k WHERE k.campaign.campaignId = :campaignId AND k.keyword = :keyword")
    int deleteByCampaign_CampaignIdANDKeyword(@Param("campaignId") Long campaignId,
                                              @Param("keyword") String exclusionKeyword);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM KeywordBid k WHERE k.campaign.campaignId IN :campaignIds")
    int deleteAllByCampaignIds(@Param("campaignIds") List<Long> campaignIds);

    boolean existsByCampaign_CampaignIdAndKeyword(Long campaignId, String keyword);
    List<KeywordBid> findAllByCampaign_CampaignId(Long campaignId);
}
