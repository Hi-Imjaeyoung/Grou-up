package growup.spring.springserver.marginforcampaign.controller;

import growup.spring.springserver.exception.marginforcampaign.MarginForCampaignIdNotFoundException;
import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.marginforcampaign.dto.MarginForCampaignResDto;
import growup.spring.springserver.marginforcampaign.dto.MfcRequestDtos;
import growup.spring.springserver.marginforcampaign.dto.MfcValidationResponseDto;
import growup.spring.springserver.marginforcampaign.service.MarginForCampaignService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marginforcam")
@AllArgsConstructor
@Slf4j
public class MarginForCampaignController {
    private final MarginForCampaignService marginForCampaignService;

    @GetMapping("/getExecutionAboutCampaign")
    public ResponseEntity<CommonResponse<List<MarginForCampaignResDto>>> getExecutionAboutCampaign(@RequestParam Long campaignId) {
        List<MarginForCampaignResDto> marginForCampaignResDtos = marginForCampaignService.marginForCampaignByCampaignId(campaignId);

        return new ResponseEntity<>(CommonResponse
                .<List<MarginForCampaignResDto>>builder("success : getExecutionAboutCampaign")
                .data(marginForCampaignResDtos)
                .build(), HttpStatus.OK);
    }

    // 내가 가진 모든 캠페인들의 옵션
    @GetMapping("/getMyAllExecution")
    public ResponseEntity<CommonResponse<List<MarginForCampaignResDto>>> getExecutionAboutAll(@AuthenticationPrincipal UserDetails userDetails) {
        List<MarginForCampaignResDto> myAllExecution = marginForCampaignService.getMyAllExecution(userDetails.getUsername());

        return new ResponseEntity<>(CommonResponse
                .<List<MarginForCampaignResDto>>builder("success : getMyAllExecution")
                .data(myAllExecution)
                .build(), HttpStatus.OK);

    }

    @PatchMapping("/updateExecutionAboutCampaign")
    public ResponseEntity<CommonResponse<MfcValidationResponseDto>> updateExecutionAboutCampaign(@Valid @RequestBody MfcRequestDtos mfcRequestDtos,
                                                                                                 @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        log.info("updateExecutionAboutCampaign : {}  " , mfcRequestDtos );
        MfcValidationResponseDto mfcValidationResponseDto = marginForCampaignService.searchMarginForCampaignProductName(email, mfcRequestDtos);
        log.info("mfcValidationResponseDto : {}", mfcValidationResponseDto);
        return new ResponseEntity<>(CommonResponse
                .<MfcValidationResponseDto>builder("success : getExecutionAboutCampaign")
                .data(mfcValidationResponseDto)
                .build(), HttpStatus.OK);
    }

    @DeleteMapping("/deleteExecutionAboutCampaign")
    public ResponseEntity<CommonResponse<Void>> deleteExecutionAboutCampaign(@RequestParam Long id) {
        try {
            marginForCampaignService.deleteMarginForCampaign(id);
            return ResponseEntity.ok(CommonResponse
                    .<Void>builder("success : delete")
                    .data(null) // 데이터가 없으므로 null
                    .build());
        } catch (MarginForCampaignIdNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CommonResponse
                    .<Void>builder(e.getMessage())
                    .data(null) // 데이터가 없으므로 null
                    .build());
        }
    }
}