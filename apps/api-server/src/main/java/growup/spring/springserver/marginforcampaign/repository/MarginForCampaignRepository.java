package growup.spring.springserver.marginforcampaign.repository;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.dto.MarginForCampaignOptionNameAndCampaignId;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarginForCampaignRepository extends JpaRepository<MarginForCampaign, Long> {

    @Query("SELECT m FROM MarginForCampaign m WHERE m.campaign.campaignId = :campaignId")
    List<MarginForCampaign> MarginForCampaignByCampaignId(Long campaignId);

    // N+1을 방지하기 위해서 EntityGraph를 사용.
    @EntityGraph(attributePaths = {"campaign"})
    List<MarginForCampaign> findByCampaignMemberEmail(String email);

    // 순수 jpa 쿼리와 비교시
    // 성능 최적화(N+1 문제 방지)와 명시성 때문에 해당 코드를 추천한다고 젬미니는 말합니다
    @Query("SELECT mfc FROM MarginForCampaign mfc JOIN FETCH mfc.campaign c WHERE c.member.email = :email")
    List<MarginForCampaign> findAllByMemberEmailWithFetch(@Param("email") String email);

    @Query("SELECT m FROM MarginForCampaign m JOIN m.campaign.member member WHERE member.email = :email AND m.mfcProductName = :productName AND m.mfcType = :mfcType AND m.campaign.campaignId <> :campaignId")
    List<MarginForCampaign> findByEmailAndMfcProductNameExcludingCampaign(@Param("email") String email,
                                                                              @Param("productName") String productName,
                                                                              @Param("campaignId") Long campaignId,
                                                                              @Param("mfcType") MarginType mfcType);

    // 해당 쿼리가 주석처리되어있던 이유가 따로 있을까요? 일단 제가 필요해서 열었습니당.
    @Query("SELECT m FROM MarginForCampaign m WHERE m.campaign.campaignId = :campaignId AND m.mfcProductName = :productName")
    Optional<MarginForCampaign> findByCampaignAndMfcProductName(@Param("productName") String productName,
                                                                @Param("campaignId") Long campaignId);

    @Query("SELECT m FROM MarginForCampaign m WHERE m.campaign.campaignId = :campaignId AND m.id = :mfcId")
    Optional<MarginForCampaign> findByCampaignAndMfcId(@Param("mfcId") Long mfcId,
                                                                @Param("campaignId") Long campaignId);

    boolean existsByCampaignAndMfcProductNameAndMfcType(Campaign campaign, String mfcProductName, MarginType mfcType);

    @Query("SELECT new growup.spring.springserver.marginforcampaign.dto.MarginForCampaignOptionNameAndCampaignId(" +
            "mfc.mfcProductName," +
            "c.campaignId)" +
            "FROM MarginForCampaign mfc " +
            "JOIN mfc.campaign c " +
            "WHERE c.member.email = :email")
    List<MarginForCampaignOptionNameAndCampaignId> findByCampaignEmail(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM MarginForCampaign mfc WHERE mfc.mfcProductName NOT IN :optionNamesInExcels AND " +
            "mfc.campaign.member.email = :email AND " +
            "mfc.campaign.campaignId = :campaignId")
    int deleteNotIncludeOptionName(
            @Param("optionNamesInExcels") List<String> optionNamesInExcels,
            @Param("email") String email,
            @Param("campaignId")Long campaignId);

    @Modifying(clearAutomatically = true,flushAutomatically = true)
    @Query("DELETE FROM MarginForCampaign mfc WHERE mfc.campaign.campaignId IN :campaignIds")
    int deleteAllByCampaignIds(@Param("campaignIds") List<Long> campaignIds);
}