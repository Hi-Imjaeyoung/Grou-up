package growup.spring.springserver.margin.controller;

import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.margin.dto.*;
import growup.spring.springserver.margin.service.MarginService;
import growup.spring.springserver.marginforcampaign.dto.MfcRequestDtos;
import growup.spring.springserver.marginforcampaign.dto.MfcRequestWithDatesDto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/margin")
@Slf4j
@AllArgsConstructor
public class MarginController {
    private final MarginService marginService;

    @GetMapping("/getCampaignAllSales")
    public ResponseEntity<CommonResponse<?>> getCampaignAllSales(@RequestParam("date") LocalDate date,
                                                                 @AuthenticationPrincipal UserDetails userDetails) {
        List<MarginSummaryResponseDto> campaignAllSales = marginService.getCampaignAllSales(userDetails.getUsername(), date);

        return new ResponseEntity<>(CommonResponse
                .<List<MarginSummaryResponseDto>>builder("success : getMyCampaignDetails")
                .data(campaignAllSales)
                .build(), HttpStatus.OK);
    }

    @GetMapping("/getDailyAdSummary")
    public ResponseEntity<CommonResponse<?>> getDailyAdSummary(@RequestParam("date") LocalDate date,
                                                               @AuthenticationPrincipal UserDetails userDetails) {
        List<DailyAdSummaryDto> byCampaignIdsAndDates = marginService.findByCampaignIdsAndDates(userDetails.getUsername(), date);

        return new ResponseEntity<>(CommonResponse
                .<List<DailyAdSummaryDto>>builder("success: getDailyAdSummary")
                .data(byCampaignIdsAndDates)
                .build(), HttpStatus.OK
        );
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

    // Return 타입 뭘로 ?. 고민
    @PatchMapping("marginUpdatesByPeriod")
    public ResponseEntity<CommonResponse<String>> marginUpdatesByPeriod(@Valid @RequestBody MfcRequestWithDatesDto mfcRequestWithDatesDto,
                                                                        @AuthenticationPrincipal UserDetails userDetails) {

        marginService.marginUpdatesByPeriod(mfcRequestWithDatesDto, userDetails.getUsername());

        return ResponseEntity.ok(CommonResponse
                .<String>builder("success: marginUpdatesByPeriod")
                .data("Margin update successful")  // 성공 메시지 반환
                .build());
    }

    @GetMapping("getDailyMarginSummary")
    public ResponseEntity<CommonResponse<List<DailyMarginSummary>>> getDailyMarginSummary(@RequestParam("date") LocalDate date,
                                                                   @AuthenticationPrincipal UserDetails userDetails) {
        List<DailyMarginSummary> dailyMarginSummary = marginService.getDailyMarginSummary(userDetails.getUsername(), date);

        return new ResponseEntity<>(CommonResponse
                .<List<DailyMarginSummary>>builder("success : getMyCampaignDetails")
                .data(dailyMarginSummary)
                .build(), HttpStatus.OK);
    }
    // 일자별 모든 캠페인의 마진을더한 총 마진.
    @GetMapping("/getNetProfit")
    public ResponseEntity<CommonResponse<List<DailyNetProfitResponseDto>>> getNetProfit(@RequestParam("startDate") LocalDate start,
                                                                                        @RequestParam("endDate") LocalDate end,
                                                                                        @AuthenticationPrincipal UserDetails userDetails) {
        List<DailyNetProfitResponseDto> dailyTotalMargin = marginService.getDailyTotalMarginListResDto(start, end, userDetails.getUsername());

        return new ResponseEntity<>(CommonResponse
                .<List<DailyNetProfitResponseDto>>builder("success : getNetProfit")
                .data(dailyTotalMargin)
                .build(), HttpStatus.OK);

    }

    // 목표효율, 광고 예산 업데이트
    @PostMapping("/updateEfficiencyAndAdBudget")
    public ResponseEntity<CommonResponse<MarginUpdateResponseDto>> updateEfficiencyAndAdBudget(@Valid @RequestBody MarginUpdateRequestDtos marginUpdateRequestDtos,
                                                                                              @AuthenticationPrincipal UserDetails userDetails) {
        MarginUpdateResponseDto marginUpdateResponseDto = marginService.updateEfficiencyAndAdBudget(marginUpdateRequestDtos);

        return new ResponseEntity<>(CommonResponse
                .<MarginUpdateResponseDto>builder("success : updateEfficiencyAndAdBudget")
                .data(marginUpdateResponseDto)
                .build(), HttpStatus.OK);
    }
}