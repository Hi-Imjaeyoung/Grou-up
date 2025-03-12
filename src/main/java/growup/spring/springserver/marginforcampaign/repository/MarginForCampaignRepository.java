package growup.spring.springserver.marginforcampaign.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarginForCampaignRepository extends JpaRepository<MarginForCampaign, Long> {

    @Query("SELECT m FROM MarginForCampaign m WHERE m.campaign.campaignId = :campaignId")
    List<MarginForCampaign> MarginForCampaignByCampaignId(Long campaignId);


    @Query("SELECT m FROM MarginForCampaign m JOIN m.campaign.member member WHERE member.email = :email AND m.mfcProductName = :productName AND m.mfcType = :mfcType AND m.campaign.campaignId <> :campaignId")
    Optional<MarginForCampaign> findByEmailAndMfcProductNameExcludingCampaign(@Param("email") String email,
                                                                              @Param("productName") String productName,
                                                                              @Param("campaignId") Long campaignId,
                                                                              @Param("mfcType") MarginType mfcType);

//    @Query("SELECT m FROM MarginForCampaign m WHERE m.campaign.campaignId = :campaignId AND m.mfcProductName = :productName")
//    Optional<MarginForCampaign> findByCampaignAndMfcProductName(@Param("productName") String productName,
//                                                                @Param("campaignId") Long campaignId);
    @Query("SELECT m FROM MarginForCampaign m WHERE m.campaign.campaignId = :campaignId AND m.id = :mfcId")
    Optional<MarginForCampaign> findByCampaignAndMfcId(@Param("mfcId") Long mfcId,
                                                                @Param("campaignId") Long campaignId);


    boolean existsByCampaignAndMfcProductNameAndMfcType(Campaign campaign, String mfcProductName, MarginType mfcType);
}