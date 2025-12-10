package growup.spring.springserver.marginforcampaignchangedbyperiod.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import growup.spring.springserver.marginforcampaign.service.MarginForCampaignService;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import growup.spring.springserver.marginforcampaignchangedbyperiod.domain.MarginForCampaignChangedByPeriod;
import growup.spring.springserver.marginforcampaignchangedbyperiod.dto.MarginChangeSaveRequestDto;
import growup.spring.springserver.marginforcampaignchangedbyperiod.repository.MarginForCampaignChangedByPeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarginForCampaignChangedByPeriodServiceTest {
    @InjectMocks
    private MarginForCampaignChangedByPeriodService marginForCampaignChangedByPeriodService;

    @Mock
    private MarginForCampaignService marginForCampaignService;
    @Mock
    private MarginForCampaignChangedByPeriodRepository marginForCampaignChangedByPeriodRepository;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private MarginForCampaignRepository marginForCampaignRepository;

    private Member member;
    private Campaign campaign1, campaign2;
    private MarginForCampaign mfc1, mfc2, mfc11;

    @BeforeEach
    void setup() {
        // 필요 객체는 직접 생성(Repo mock 불필요)
        member = Member.builder().email("test@test.com").build();

        campaign1 = Campaign.builder()
                .campaignId(1L)
                .camCampaignName("캠페인1")
                .member(member)
                .build();

        campaign2 = Campaign.builder()
                .campaignId(2L)
                .camCampaignName("캠페인2")
                .member(member)
                .build();

        mfc1 = MarginForCampaign.builder()
                .id(1L)
                .mfcProductName("상품1")
                .mfcTotalPrice(1L)
                .mfcCostPrice(1L)
                .mfcPerPiece(1L)
                .mfcZeroRoas(1.1)
                .campaign(campaign1)
                .mfcType(MarginType.ROCKET_GROWTH)
                .build();

        mfc11 = MarginForCampaign.builder()
                .id(11L)
                .mfcProductName("상품1-1")
                .mfcTotalPrice(1L)
                .mfcCostPrice(1L)
                .mfcPerPiece(1L)
                .mfcZeroRoas(1.1)
                .campaign(campaign1)
                .mfcType(MarginType.ROCKET_GROWTH)
                .build();

        mfc2 = MarginForCampaign.builder()
                .id(2L)
                .mfcProductName("상품2")
                .mfcTotalPrice(1L)
                .mfcCostPrice(1L)
                .mfcPerPiece(1L)
                .mfcZeroRoas(1.1)
                .campaign(campaign2)
                .mfcType(MarginType.ROCKET_GROWTH)
                .build();
    }

    @Test
    @DisplayName("save() — 기존 1/1~1/3 기록 존재, 1/4만 신규 생성 & 전체 saveAll 호출")
    void saveTest1() {

        MarginChangeSaveRequestDto dto = dto(1L,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 4),
                1000L, 2000L, 500L, 100L);

        List<MarginForCampaignChangedByPeriod> existingRecords = List.of(
                newRecord(1L,mfc1, LocalDate.of(2025, 1, 1)),
                newRecord(1L,mfc1, LocalDate.of(2025, 1, 2)),
                newRecord(1L,mfc1, LocalDate.of(2025, 1, 3))
        );

        when(marginForCampaignService.getMyMarginForCampaignById(1L)).thenReturn(mfc1);
        when(marginForCampaignChangedByPeriodRepository.findAllByMarginForCampaign_IdAndDateRange(
                eq(mfc1.getId()), any(), any())).thenReturn(existingRecords);

        marginForCampaignChangedByPeriodService.save(dto);

        // 기존 레코드들이 업데이트 되었는지 검증
        existingRecords.forEach(rcd ->
                assertEquals(dto.salePrice(), rcd.getSalePrice())
        );

        verify(marginForCampaignChangedByPeriodRepository, times(1))
                .saveAll(anyList());
    }

    @Test
    @DisplayName("save() — 기존 데이터 없으면 모든 날짜 신규 생성 후 saveAll 호출")
    void saveTest2() {

        MarginChangeSaveRequestDto dto = dto(1L,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 4),
                1000L, 2000L, 500L, 100L);

        when(marginForCampaignService.getMyMarginForCampaignById(1L)).thenReturn(mfc1);
        when(marginForCampaignChangedByPeriodRepository.findAllByMarginForCampaign_IdAndDateRange(
                eq(mfc1.getId()), any(), any())).thenReturn(List.of());

        marginForCampaignChangedByPeriodService.save(dto);

        verify(marginForCampaignChangedByPeriodRepository, times(1))
                .saveAll(anyList());
    }

    @Test
    @DisplayName("findAllByMfcCbpIdsAndDateRange() — 성공 케이스")
    void findAllByMfcCbpIdsAndDateRange_success() {

        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 3);
        List<Long> mfcIds = List.of(1L, 2L);

        List<MarginForCampaignChangedByPeriod> existing = List.of(
                newRecord(1L,mfc1, LocalDate.of(2025, 1, 1)),
                newRecord(2L,mfc11, LocalDate.of(2025, 1, 1)),
                newRecord(3L,mfc1, LocalDate.of(2025, 1, 2)),
                newRecord(4L,mfc11, LocalDate.of(2025, 1, 13))
        );

        when(marginForCampaignChangedByPeriodRepository.findAllByMfcIdsAndDateRange(
                any(), any(), any())).thenReturn(existing);

        Map<LocalDate, Map<Long, MarginForCampaignChangedByPeriod>> result =
                marginForCampaignChangedByPeriodService.findAllByMfcCbpIdsAndDateRange(
                        mfcIds, startDate, endDate);
        System.out.println("result = " + result);
        assertThat(result).hasSize(3);
    }
    private MarginForCampaignChangedByPeriod newRecord(Long id ,MarginForCampaign mfc, LocalDate date) {
        return MarginForCampaignChangedByPeriod.builder()
                .id(id)
                .marginForCampaign(mfc)
                .date(date)
                .salePrice(1000L)
                .totalPrice(2000L)
                .costPrice(500L)
                .returnPrice(100L)
                .build();
    }
    private MarginChangeSaveRequestDto dto(Long mfcId, LocalDate startDate, LocalDate endDate,
                                           Long salePrice, Long totalPrice, Long costPrice, Long returnPrice) {
        return new MarginChangeSaveRequestDto(
                mfcId,
                startDate,
                endDate,
                salePrice,
                totalPrice,
                costPrice,
                returnPrice
        );
    }
    public Campaign getCampaign(String name, Long id, Member member) {
        return Campaign.builder()
                .campaignId(id)
                .camCampaignName(name)
                .member(member)
                .build();
    }
}