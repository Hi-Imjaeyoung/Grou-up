package growup.spring.springserver.margin.controller;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.exception.global.RequestException;
import growup.spring.springserver.exception.login.MemberNotFoundException;
import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.global.dto.req.DateRangeRequest;
import growup.spring.springserver.margin.dto.*;
import growup.spring.springserver.margin.service.MarginService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/margin")
@Slf4j
@AllArgsConstructor
public class MarginController {
    private final MarginService marginService;
    private final CampaignService campaignService;
    /*
     * TODO
     *  매출보고서(3사분)
     *  실패시 빈 리스트 리턴
     * */
    @GetMapping("/getCampaignAllSales")
    public ResponseEntity<CommonResponse<List<MarginSummaryResponseDto>>> getCampaignAllSales(@RequestParam("date") LocalDate date,
                                                                                              @AuthenticationPrincipal UserDetails userDetails) {
        List<MarginSummaryResponseDto> campaignAllSales;
        try {
            campaignAllSales = marginService.getCampaignAllSales(userDetails.getUsername(), date);
        } catch (CampaignNotFoundException campaignNotFoundException) {
            campaignAllSales = new ArrayList<>();
        }

        return new ResponseEntity<>(CommonResponse
                .<List<MarginSummaryResponseDto>>builder("success : getMyCampaignDetails")
                .data(campaignAllSales)
                .build(), HttpStatus.OK);
    }

    @GetMapping("/getMargin")
    public ResponseEntity<CommonResponse<List<MarginResponseDto>>> getMargin(@RequestParam("startDate") LocalDate start,
                                                                             @RequestParam("endDate") LocalDate end,
                                                                             @RequestParam("campaignId") Long campaignId,
                                                                             @AuthenticationPrincipal UserDetails userDetails) {

        List<MarginResponseDto> marginResponseDtos = marginService.getALLMargin(start, end, campaignId, userDetails.getUsername());
        return new ResponseEntity<>(CommonResponse
                .<List<MarginResponseDto>>builder("success :getMargin ")
                .data(marginResponseDtos)
                .build(), HttpStatus.OK);
    }


    /*TODO
     *  마진보고서 (4사분면)
     *  실패시 빈 리스트 리턴*/
    @GetMapping("/getDailyMarginSummary")
    public ResponseEntity<CommonResponse<List<DailyMarginSummary>>> getDailyMarginSummary(@Valid@ModelAttribute DateRangeRequest dateRangeRequest,
                                                                                          @AuthenticationPrincipal UserDetails userDetails) {
        List<DailyMarginSummary> dailyMarginSummary;
        List<Campaign> campaigns = campaignService.getCampaignsByEmail(userDetails.getUsername());
        try {
            dailyMarginSummary = marginService.getDailyMarginSummary(campaigns, dateRangeRequest.getStart(),dateRangeRequest.getEnd());
        } catch (CampaignNotFoundException | MemberNotFoundException exception) {
            dailyMarginSummary = new ArrayList<>();
        }

        return new ResponseEntity<>(CommonResponse
                .<List<DailyMarginSummary>>builder("success : getMyCampaignDetails")
                .data(dailyMarginSummary)
                .build(), HttpStatus.OK);
    }

    // 일자별 모든 캠페인의 마진을더한 총 마진 및 반품
    @GetMapping("/getNetProfitAndReturnCost")
    public ResponseEntity<CommonResponse<List<DailyNetProfitResponseDto>>> getNetProfit(@RequestParam("startDate") LocalDate start,
                                                                                        @RequestParam("endDate") LocalDate end,
                                                                                        @AuthenticationPrincipal UserDetails userDetails) {
        List<DailyNetProfitResponseDto> dailyTotalMargin = marginService.getDailyTotalMarginListResDto(start, end, userDetails.getUsername());
        return new ResponseEntity<>(CommonResponse
                .<List<DailyNetProfitResponseDto>>builder("success : getNetProfitAndReturnCost")
                .data(dailyTotalMargin)
                .build(), HttpStatus.OK);

    }

    // 목표효율, 광고 예산 업데이트
    @PostMapping("/updateEfficiencyAndAdBudget")
    public ResponseEntity<CommonResponse<MarginUpdateResponseDto>> updateEfficiencyAndAdBudget(@Valid @RequestBody MarginUpdateRequestDtos marginUpdateRequestDtos,
                                                                                               @AuthenticationPrincipal UserDetails userDetails,
                                                                                               BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new RequestException();
        }

        MarginUpdateResponseDto marginUpdateResponseDto = marginService.updateEfficiencyAndAdBudget(marginUpdateRequestDtos);

        return new ResponseEntity<>(CommonResponse
                .<MarginUpdateResponseDto>builder("success : updateEfficiencyAndAdBudget")
                .data(marginUpdateResponseDto)
                .build(), HttpStatus.OK);
    }

    /**
     * 마진 테이블 사용자가 생성
     *
     * @param targetDate
     * @param campaignId
     * @return margin_ID
     */

    @PostMapping("/createMarginTable")
    public ResponseEntity<CommonResponse<Long>> createMarginTable(@RequestParam("targetDate") LocalDate targetDate,
                                                                  @RequestParam("campaignId") Long campaignId,
                                                                  @AuthenticationPrincipal UserDetails userDetails) {
        Long marginId = marginService.createMarginTable(targetDate, campaignId, userDetails.getUsername());
        return new ResponseEntity<>(CommonResponse
                .<Long>builder("success : createMarginTable")
                .data(marginId)
                .build(), HttpStatus.OK);
    }

    /**
     * 마진 결과 보기에서 작성한 목표효율, 광고예산 만
     * 캠페인 분석 표 (statGraph3) 에 같이 보여주고자 함.
     *
     * @param startDate
     * @param endDate
     * @param campaignId
     * @return List<SimpleMarginResponseDto>
     */

    @GetMapping("/getMarginSimple")
    public ResponseEntity<CommonResponse<SimpleMarginResponseForStaticGraph3Dto>> getMarginSimple(
            @RequestParam("startDate") LocalDate start,
            @RequestParam("endDate") LocalDate end,
            @RequestParam("campaignId") Long campaignId,
            @AuthenticationPrincipal UserDetails userDetails) {

        SimpleMarginResponseForStaticGraph3Dto simpleMargins = marginService.getSimpleMargin(start, end, campaignId, userDetails.getUsername());

        return ResponseEntity.ok(CommonResponse
                .<SimpleMarginResponseForStaticGraph3Dto>builder("success : getMarginSimple")
                .data(simpleMargins)
                .build());
    }

    @GetMapping("/findLatestMarginDateByEmail")
    public ResponseEntity<CommonResponse<LocalDate>> findLatestMarginDateByEmail(@AuthenticationPrincipal UserDetails userDetails) {
        LocalDate latestMarginDate = marginService.findLatestMarginDateByEmail(userDetails.getUsername());
        return ResponseEntity.ok(CommonResponse
                .<LocalDate>builder("success : findLatestMarginDateByEmail")
                .data(latestMarginDate)
                .build());
    }

    @GetMapping("/getMarginOverview")
    public ResponseEntity<CommonResponse<List<MarginOverviewResponseDto>>> getMarginOverview(
            @Valid @ModelAttribute DateRangeRequest dateRangeReq,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<MarginOverviewResponseDto> marginOverview = marginService.getMarginOverview(dateRangeReq.getStart(), dateRangeReq.getEnd(), userDetails.getUsername());

        return ResponseEntity.ok(CommonResponse
                .<List<MarginOverviewResponseDto>>builder("success : getMarginOverview")
                .data(marginOverview)
                .build());
    }

    @GetMapping("/getMarginOverviewGraph")
    public ResponseEntity<CommonResponse<List<DailyAdSummaryDto>>> getMarginOverviewGraph(
        @Valid @ModelAttribute DateRangeRequest dateRangeReq,
        @AuthenticationPrincipal UserDetails userDetails) {

        List<DailyAdSummaryDto> marginOverviewGraph = marginService.getMarginOverviewGraph(
                dateRangeReq.getStart(),
                dateRangeReq.getEnd(),
                userDetails.getUsername()
        );

        return ResponseEntity.ok(CommonResponse
                .<List<DailyAdSummaryDto>>builder("success : getMarginOverviewGraph")
                .data(marginOverviewGraph)
                .build());
    }
}