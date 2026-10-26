package growup.spring.springserver.execution.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.execution.domain.Execution;
import growup.spring.springserver.execution.repository.ExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import growup.spring.springserver.exception.campaignoptiondetails.CampaignOptionNotFoundException;
import growup.spring.springserver.execution.domain.Execution;
import growup.spring.springserver.execution.dto.ExecutionDto;
import growup.spring.springserver.execution.dto.ExecutionMarginResDto;
import growup.spring.springserver.execution.dto.ExecutionRequestDtos;
import growup.spring.springserver.execution.dto.ExecutionResponseDto;
import growup.spring.springserver.execution.repository.ExecutionRepository;
import growup.spring.springserver.login.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

    @InjectMocks
    private ExecutionService executionService;

    @Mock
    ExecutionRepository executionRepository;
  
    private Campaign campaign;
    
    @BeforeEach
    void setUp(){
        campaign = Campaign.builder()
                .camCampaignName("test")
                .campaignId(1L)
                .build();
    }
    @Test
    @DisplayName("executionMarginResDto() ErrorCase1. 캠패인 존재 하지 않을때 ")
    void executionMarginResDto_ErrorCase1() {
        // given
        doReturn(List.of()).when(executionRepository).findExecutionByCampaignId(any(Long.class));

        //when
        List<ExecutionMarginResDto> executionMarginResDtos = executionService.getMyExecutionData(any(Long.class));

        //then
        assertThat(executionMarginResDtos).isEmpty();

    }



//    @DisplayName("getExecutionsByCampaignIdAndExeIds() : 빈값이 조회 결과일 때")
//    @Test
//    void test1_1(){
//        //given
//        doReturn(List.of()).when(executionRepository).findByCampaignIdAndExeId(any(Long.class),any(List.class));
//        final List<Long> request = List.of(1L,2L,3L);
//        //when
//        final List<Execution> result = executionService.getExecutionsByCampaignIdAndExeIds(1L,request);
//        //then
//        assertThat(result).hasSize(0);
//    }
//
//    @DisplayName("getExecutionsByCampaignIdAndExeIds() : 빈값을 조회 시")
//    @Test
//    void test1_2(){
//        //given
//        doReturn(List.of()).when(executionRepository).findByCampaignIdAndExeId(any(Long.class),any(List.class));
//        final List<Long> request = List.of();
//        //when
//        final List<Execution> result = executionService.getExecutionsByCampaignIdAndExeIds(1L,request);
//        //then
//        assertThat(result).hasSize(0);
//    }
//
//    @DisplayName("getExecutionsByCampaignIdAndExeIds() : Success")
//    @Test
//    void test1_3(){
//        //given
//        doReturn(List.of(newExecution(campaign,1L,"details","name1"),
//                newExecution(campaign,2L,"details","name2"),
//                newExecution(campaign,3L,"details","name3"))).when(executionRepository).findByCampaignIdAndExeId(any(Long.class),any(List.class));
//        final List<Long> request = List.of(1L,2L,3L);
//        //when
//        final List<Execution> result = executionService.getExecutionsByCampaignIdAndExeIds(1L,request);
//        //then
//        assertThat(result).hasSize(3);
//    }
//
//    public Execution newExecution(Campaign campaign, long l, String detail, String name) {
//        return Execution.builder()
//                .exeId(l)
//                .exeDetailCategory(detail)
//                .exeProductName(name)
//                .campaign(campaign)
//                .build();
//    }
          
    @Test
    @DisplayName("executionMarginResDto() SuccessCase1. executionMarginResDto 반환 완료")
    void executionMarginResDto_SuccessCase1() {
        // given
        List<Execution> mockExecutions = List.of(
                newExecution(1L, 1L, "빨강색", "모자", getCampaign(1L, "모자")),
                newExecution(2L, 2L, "파랑색", "모자1", getCampaign(1L, "모자2"))
        );
        doReturn(mockExecutions)
                .when(executionRepository).findExecutionByCampaignId(any(Long.class));
        // when
        List<ExecutionMarginResDto> executionMarginResDtos = executionService.getMyExecutionData(1L);
        System.out.println("executionMarginResDtos = " + executionMarginResDtos);

        // then
        assertThat(executionMarginResDtos).hasSize(2);
        System.out.println("executionMarginResDtos = " + executionMarginResDtos);
        assertThat(executionMarginResDtos.get(0)).extracting("exeId").isEqualTo(1L);
        assertThat(executionMarginResDtos.get(0)).extracting("exeDetailCategory").isEqualTo("모자");

        assertThat(executionMarginResDtos.get(1)).extracting("exeId").isEqualTo(2L);
        assertThat(executionMarginResDtos.get(1)).extracting("exeDetailCategory").isEqualTo("모자1");
    }

    @Test
    @DisplayName("updateExecutions() Success Case - 정상적으로 실행 업데이트")
    void updateExecutions_Success() {
        // given
        Campaign mockCampaign = getCampaign(1L, "모자");
        Execution mockExecution = newExecution(1L, 100L, "빨강색", "모자", mockCampaign);

        Long beforeSalePrice = mockExecution.getExeSalePrice();
        Long beforeTotalPrice = mockExecution.getExeTotalPrice();
        Long beforeCostPrice = mockExecution.getExeCostPrice();
        Double beforePerPiece = mockExecution.getExePerPiece();
        Double beforeZeroRoas = mockExecution.getExeZeroRoas();

        ExecutionRequestDtos requestDtos = createExecutionRequestDtos(1L, List.of(
                createExecutionDto(100L, 6000L, 12000L, 8000L, 12.0, 4.0)
        ));

        doReturn(Optional.of(mockExecution)) // doReturn 사용
                .when(executionRepository).findByCampaign_CampaignIdAndExeId(1L, 100L);

        // when
        ExecutionResponseDto responseDto = executionService.updateExecutions(requestDtos, mockCampaign);

        // then
        assertEquals(1, responseDto.getRequestNumber());
        assertEquals(1, responseDto.getResponseNumber());

        assertNotEquals(beforeSalePrice, mockExecution.getExeSalePrice());
        assertNotEquals(beforeTotalPrice, mockExecution.getExeTotalPrice());
        assertNotEquals(beforeCostPrice, mockExecution.getExeCostPrice());
        assertNotEquals(beforePerPiece, mockExecution.getExePerPiece());
        assertNotEquals(beforeZeroRoas, mockExecution.getExeZeroRoas());

        assertEquals(6000L, mockExecution.getExeSalePrice());
        assertEquals(12000L, mockExecution.getExeTotalPrice());
        assertEquals(8000L, mockExecution.getExeCostPrice());
        assertEquals(12.0, mockExecution.getExePerPiece());
        assertEquals(4.0, mockExecution.getExeZeroRoas());
    }


    @Test
    @DisplayName("updateExecutions() Success Case - 3개 중 2개만 성공")
    void updateExecutions_PartialSuccess() {
        // given
        Campaign mockCampaign = getCampaign(1L, "모자");
        Execution mockExecution1 = newExecution(1L, 100L, "빨강색", "모자", mockCampaign);
        Execution mockExecution2 = newExecution(2L, 101L, "파랑색", "모자", mockCampaign);

        ExecutionRequestDtos requestDtos = createExecutionRequestDtos(1L, List.of(
                createExecutionDto(100L, 6000L, 12000L, 8000L, 12.0, 4.0),
                createExecutionDto(101L, 7000L, 14000L, 9000L, 14.0, 5.0),
                createExecutionDto(102L, 8000L, 16000L, 10000L, 16.0, 6.0) // 존재하지 않는 execution
        ));

        doReturn(Optional.of(mockExecution1))
                .when(executionRepository).findByCampaign_CampaignIdAndExeId(1L, 100L);
        doReturn(Optional.of(mockExecution2))
                .when(executionRepository).findByCampaign_CampaignIdAndExeId(1L, 101L);
        doReturn(Optional.empty())
                .when(executionRepository).findByCampaign_CampaignIdAndExeId(1L, 102L);

        // when
        ExecutionResponseDto responseDto = executionService.updateExecutions(requestDtos, mockCampaign);

        // then
        assertEquals(3, responseDto.getRequestNumber());
        assertEquals(2, responseDto.getResponseNumber());
    }

    public Member getMember() {
        return Member.builder()
                .email("test@test.com")
                .build();
    }

    public Execution newExecution(long l, long executionid, String detail, String name, Campaign campaign) {
        return Execution.builder()
                .id(l)
                .exeId(executionid)
                .exeDetailCategory(detail)
                .exeProductName(name)
                .campaign(campaign)
                .build();
    }

    public Campaign getCampaign(Long id, String title) {
        return Campaign.builder()
                .campaignId(id)
                .camCampaignName(title)
                .build();
    }
    public ExecutionDto createExecutionDto(Long executionId, Long salePrice, Long totalPrice, Long costPrice, Double perPiece, Double zeroRoas) {
        return ExecutionDto.builder()
                .executionId(executionId)
                .exeSalePrice(salePrice)
                .exeTotalPrice(totalPrice)
                .exeCostPrice(costPrice)
                .exePerPiece(perPiece)
                .exeZeroRoas(zeroRoas)
                .build();
    }

    public ExecutionRequestDtos createExecutionRequestDtos(Long campaignId, List<ExecutionDto> executionDtos) {
        return ExecutionRequestDtos.builder()
                .campaignId(campaignId)
                .data(executionDtos)
                .build();
    }
}
