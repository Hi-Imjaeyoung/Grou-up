package growup.spring.springserver.campaign.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.login.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign,Long> {

    Optional<Campaign> findByCampaignId(Long Long);

    List<Campaign> findAllByMember(Member member);
    @Query("SELECT c FROM Campaign c WHERE c.member.email = :email")
    List<Campaign> findAllByEmail(@Param("email") String email);
    @Query("SELECT c FROM Campaign c WHERE c.campaignId = :campaignId AND c.member.email = :email")
    Optional<Campaign> findByCampaignIdANDEmail(@Param("campaignId") Long campaignID,
                                                @Param("email") String email);

    @Modifying(clearAutomatically = true,flushAutomatically = true)
    @Query("DELETE FROM Campaign c WHERE c.campaignId IN :campaignIds")
    int deleteAllByCampaignIds(@Param("campaignIds") List<Long> campaignIds);
}
