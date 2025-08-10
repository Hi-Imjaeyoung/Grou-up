package growup.spring.springserver.marginforcampaign.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.repository.CampaignRepository;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.marginforcampaign.MarginForCampaignIdNotFoundException;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaign.dto.MarginForCampaignResDto;
import growup.spring.springserver.marginforcampaign.dto.MfcDto;
import growup.spring.springserver.marginforcampaign.dto.MfcRequestDtos;
import growup.spring.springserver.marginforcampaign.dto.MfcValidationResponseDto;
import growup.spring.springserver.marginforcampaign.repository.MarginForCampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarginForCampaignServiceTest {
    @Mock
    private MarginForCampaignRepository marginForCampaignRepository;
    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private MarginForCampaignService marginForCampaignService;
    @Mock
    private CampaignService campaignService;


    private Member mockMember;
    private Campaign mockCampaign;

    @BeforeEach
    void setUp() {
        mockMember = getMember();
        mockCampaign = getCampaign("송보석", 1L);
    }

    @Test
    @DisplayName("get MarginForCampaignByCampaignId Success 1. Empty")
    void MarginForCampaignByCampaignId_Success1() {
        doReturn(List.of()).when(marginForCampaignRepository).MarginForCampaignByCampaignId(1L);

        final List<MarginForCampaignResDto> result = marginForCampaignService.marginForCampaignByCampaignId(1L);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("get MarginForCampaignByCampaignId Success 2. ")
    void MarginForCampaignByCampaignId_Success2() {
        //given
        Campaign campaign = getCampaign("송보석", 1L);
        doReturn(List.of(
                getMarginForCampaign(campaign, "모자1", 1L, 1L, 1L, 1.1),
                getMarginForCampaign(campaign, "모자2", 1L, 1L, 1L, 1.1),
                getMarginForCampaign(campaign, "모자3", 1L, 1L, 1L, 1.1)))
                .when(marginForCampaignRepository).MarginForCampaignByCampaignId(1L);

        //when
        final List<MarginForCampaignResDto> result = marginForCampaignService.marginForCampaignByCampaignId(1L);

        // given
        assertThat(result).hasSize(3);


    }

    @DisplayName("모든 상품이 존재하지 않을 경우")
    @Test
    void searchMarginForCampaignProductName_Success1() {
        // given
        MfcRequestDtos requestDtos = getRequestDtos(1L, List.of(
                getMfcDto("상품1", 1L, 1L, 1L, 1.1),
                getMfcDto("상품2", 1L, 1L, 1L, 1.1)
        ));
        when(campaignService.getMyCampaign(anyLong(),anyString())).thenReturn(mockCampaign);
        when(marginForCampaignRepository.findByEmailAndMfcProductNameExcludingCampaign(any(), any(), any(),any()))
                .thenReturn(Optional.empty()); // 모든 상품이 존재하지 않음
        // when
        MfcValidationResponseDto response = marginForCampaignService.searchMarginForCampaignProductName("fa7271@naver.com", requestDtos);

        // then
        assertThat(response.getFailedProductNames()).isEmpty(); // 실패한 상품이 없어야 함
        assertThat(response.getResponseNumber()).isEqualTo(2); // 두 개의 상품이 성공적으로 추가되었어야 함
    }

    @DisplayName("일부 상품이 존재할 경우")
    @Test
    void searchMarginForCampaignProductName_Success2() {
        // given
        Campaign campaign2 = getCampaign("임재영", 2L);
        MfcRequestDtos requestDtos = getRequestDtos(1L, List.of(
                getMfcDto("존재하지 않는 상품", 1L, 1L, 1L, 1.1),
                getMfcDto("존재하는 상품", 1L, 1L, 1L, 1.1)
        ));

        when(marginForCampaignRepository.findByEmailAndMfcProductNameExcludingCampaign(any(), eq("존재하는 상품"), any(),any()))
                .thenReturn(Optional.of(getMarginForCampaign(campaign2, "존재하는상품", 1L, 1L, 1L, 1.1))); // 존재하는 상품
        when(marginForCampaignRepository.findByEmailAndMfcProductNameExcludingCampaign(any(), eq("존재하지 않는 상품"), any(),any()))
                .thenReturn(Optional.empty()); // 존재하지 않는 상품
        when(campaignService.getMyCampaign(anyLong(),anyString())).thenReturn(campaign2);

        // when
        MfcValidationResponseDto response = marginForCampaignService.searchMarginForCampaignProductName("fa7271@naver.com", requestDtos);

        // then
        assertThat(response.getFailedProductNames()).containsExactly("존재하는 상품"); // 실패한 상품에 존재하는 상품이 있어야 함
        assertThat(response.getResponseNumber()).isEqualTo(1); // 새로운 상품이 하나 추가되었어야 함
    }
    @DisplayName("모든 상품이 현재 캠페인에 존재하는 경우")
    @Test
    void searchMarginForCampaignProductName_Success3() {
        // given
        when(campaignService.getMyCampaign(anyLong(), anyString())).thenReturn(mockCampaign);

        MfcRequestDtos requestDtos = getRequestDtos(1L, List.of(
                getMfcDto("상품1", 2L, 2L, 1L, 1.1),
                getMfcDto("상품2", 2L, 2L, 1L, 1.1)
        ));

        when(marginForCampaignRepository.existsByCampaignAndMfcProductNameAndMfcType(any(), eq("상품1"),any())).thenReturn(true);
        when(marginForCampaignRepository.existsByCampaignAndMfcProductNameAndMfcType(any(), eq("상품2"),any())).thenReturn(true);

        // when
        MfcValidationResponseDto response = marginForCampaignService.searchMarginForCampaignProductName("test@naver.com", requestDtos);

        // then
        assertThat(response.getFailedProductNames()).containsExactlyInAnyOrder("상품1", "상품2"); // 실패 리스트에 포함
        assertThat(response.getResponseNumber()).isZero(); // 성공한 상품 없음
    }
    @DisplayName("요청한 상품명이 중복된 경우")
    @Test
    void searchMarginForCampaignProductName_Success4() {
        // given
        Campaign campaign = getCampaign("임재영", 2L);
        MfcRequestDtos requestDtos = getRequestDtos(1L, List.of(
                getMfcDto("중복 상품", 1L, 1L, 1L, 1.1),
                getMfcDto("중복 상품", 2L, 2L, 2L, 2.2)
        ));

        when(campaignService.getMyCampaign(anyLong(), anyString())).thenReturn(campaign);
        when(marginForCampaignRepository.findByEmailAndMfcProductNameExcludingCampaign(any(), eq("중복 상품"), any(),any()))
                .thenReturn(Optional.empty());

        // when
        MfcValidationResponseDto response = marginForCampaignService.searchMarginForCampaignProductName("test@naver.com", requestDtos);

        // then
        assertThat(response.getFailedProductNames()).containsExactly("중복 상품"); // 중복된 상품은 실패해야 함
        assertThat(response.getResponseNumber()).isEqualTo(1); // 하나만 추가됨
    }

    @DisplayName("Update 상품이 존재 하는 경우 ")
    @Test
    void searchMarginForCampaignProductName_Success5() {
        // given

        when(campaignService.getMyCampaign(anyLong(),anyString())).thenReturn(mockCampaign);

        MfcRequestDtos requestDtos = getRequestDtos(1L, List.of(
                getMfcDto("상품1", 2L, 2L, 1L, 1.1) // 가격이 다름
        ));
        // 다른 캠페인에는 해당 상품이 없음
        when(marginForCampaignRepository.findByEmailAndMfcProductNameExcludingCampaign(any(), eq("상품1"), any(),any()))
                .thenReturn(Optional.empty()); // 존재하는 상품


        // when
        MfcValidationResponseDto response = marginForCampaignService.searchMarginForCampaignProductName("fa7271@naver.com", requestDtos);
        // then
        assertThat(response.getFailedProductNames()).isEmpty();
        assertThat(response.getResponseNumber()).isEqualTo(1); // 성공 카운트는 1이어야 함

    }
    @DisplayName("deleteMarginForCampaign() : FailCase - 없는 ID")
    @Test
    void deleteMarginForCampaign_FailCase_NotFound() {
        // Arrange
        Long nonExistingId = 99L; // 존재하지 않는 ID
        when(marginForCampaignRepository.existsById(nonExistingId)).thenReturn(false); // ID가 존재하지 않는다고 설정

        // Act & Assert
        assertThatThrownBy(() -> marginForCampaignService.deleteMarginForCampaign(nonExistingId))
                .isInstanceOf(MarginForCampaignIdNotFoundException.class) // 예외가 발생하는지 확인
                .hasMessageContaining("없는 ID 입니다."); // 메시지 확인
    }

    @DisplayName("deleteMarginForCampaign() : Success")
    @Test
    void deleteMarginForCampaign_Success() {
        Long existingId = 1L;
        when(marginForCampaignRepository.existsById(existingId)).thenReturn(true); // ID가 존재한다고 설정

        marginForCampaignService.deleteMarginForCampaign(existingId); // 서비스 메소드 호출

        verify(marginForCampaignRepository, times(1)).deleteById(existingId); // deleteById 메소드가 한 번 호출되었는지 확인

    }

    @DisplayName("getMyAllExecution : success")
    @Test
    void getMyAllExecution_Success() {
        Member member = getMember();
        Campaign campaign = getCampaign("송보석1", 1L, member);
        Campaign campaign2 = getCampaign("송보석2", 2L,member);
        Campaign campaign3 = getCampaign("송보석3", 3L,member);
        doReturn(List.of(
                getMarginForCampaign(campaign, "모자1", 1L, 1L, 1L, 1.1),
                getMarginForCampaign(campaign2, "모자2", 1L, 1L, 1L, 1.1),
                getMarginForCampaign(campaign3, "모자3", 1L, 1L, 1L, 1.1)))
                .when(marginForCampaignRepository).findAllByMemberEmailWithFetch("fa7271@naver.com");

        List<MarginForCampaignResDto> result = marginForCampaignService.getMyAllExecution("fa7271@naver.com");

        // then
        assertThat(result)
                .extracting(MarginForCampaignResDto::getMfcProductName)
                .containsExactly("모자1", "모자2", "모자3");
    }
    @DisplayName("getMyAllExecution : empty")
    @Test
    void getMyAllExecution_Empty() {
        // given
        doReturn(List.of()).when(marginForCampaignRepository).findAllByMemberEmailWithFetch("fa7271@naver.com");
        // when
        List<MarginForCampaignResDto> result = marginForCampaignService.getMyAllExecution("fa7271@naver.com");
        // then
        assertThat(result).isEmpty(); // 결과가 비어 있어야 함
    }

    public Member getMember() {
        return Member.builder()
                .email("fa7271@naver.com")
                .build();
    }

    public Campaign getCampaign(String name, Long id) {
        return Campaign.builder()
                .campaignId(id)
                .camCampaignName(name)
                .build();
    }
    public Campaign getCampaign(String name, Long id, Member member) {
        return Campaign.builder()
                .campaignId(id)
                .member(member)
                .camCampaignName(name)
                .build();
    }

    public MarginForCampaign getMarginForCampaign(Campaign campaign, String productName, Long mfcTotalPrice, Long mfcCostPrice, Long mfcPerPiece, Double mfcZeroRoas) {
        return MarginForCampaign.builder()
                .mfcProductName(productName)
                .mfcTotalPrice(mfcTotalPrice)
                .mfcCostPrice(mfcCostPrice)
                .mfcPerPiece(mfcPerPiece)
                .mfcZeroRoas(mfcZeroRoas)
                .campaign(campaign)
                .build();
    }

    public MfcRequestDtos getRequestDtos(Long id, List<MfcDto> data) {
        return MfcRequestDtos.builder()
                .campaignId(id)
                .data(data)
                .build();
    }

    public MfcDto getMfcDto(String mfcProductName,Long mfcTotalPrice,Long mfcCostPrice,Long mfcPerPiece,Double mfcZeroRoas) {
        return MfcDto.builder()
                .mfcProductName(mfcProductName)
                .mfcTotalPrice(mfcTotalPrice)
                .mfcCostPrice(mfcCostPrice)
                .mfcPerPiece(mfcPerPiece)
                .mfcZeroRoas(mfcZeroRoas)
                .build();
    }

}