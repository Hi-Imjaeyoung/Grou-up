package growup.spring.springserver.campaignoptiondetails.controller;

import growup.spring.springserver.campaignoptiondetails.dto.CampaignOptionDetailsResponseDto;
import growup.spring.springserver.campaignoptiondetails.service.CampaignOptionDetailsService;
import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.global.dto.req.DateRangeRequest;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated  // @ValidDateRange 검증 동작
@RestController
@RequestMapping("/api/cod/")
@RequiredArgsConstructor
public class CampaignOptionDetailsController {

    private final CampaignOptionDetailsService campaignOptionDetailsService;

    @GetMapping("/getMyCampaignDetails")
    public ResponseEntity<CommonResponse<List<CampaignOptionDetailsResponseDto>>> getKeywordAboutCampaign(
            @Valid @ModelAttribute DateRangeRequest dateRangeReq, // DTO 바인딩 + 검증
            @RequestParam("campaignId") @NonNull Long campaignId
    ) {
        List<CampaignOptionDetailsResponseDto> campaignDetails =
                campaignOptionDetailsService.getCampaignDetailsByCampaignsIds(
                        dateRangeReq.getStart(),
                        dateRangeReq.getEnd(),
                        campaignId
                );

        return new ResponseEntity<>(
                CommonResponse.<List<CampaignOptionDetailsResponseDto>>builder("success : getMyCampaignDetails")
                        .data(campaignDetails)
                        .build(),
                HttpStatus.OK
        );
    }
}

