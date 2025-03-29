package growup.spring.springserver.campaign.controller;

import growup.spring.springserver.campaign.dto.CampaignResponseDto;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.global.common.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequestMapping("/api/campaign")
@RestController
public class CampaignController {
    @Autowired
    private CampaignService campaignService;

    @GetMapping("/getMyCampaigns")
    public ResponseEntity<CommonResponse<List<CampaignResponseDto>>> getMyCampaigns(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Start getMyCampaigns API target is :" + userDetails.getUsername());
        List<CampaignResponseDto> data;
        try {
            data = campaignService.getMyCampaigns(userDetails.getUsername());
        } catch (CampaignNotFoundException campaignNotFoundException){
            data = new ArrayList<>();
        } // membernotException은 잡지 않음

        log.info("End getMyCampaigns API target is :" + userDetails.getUsername());
        return new ResponseEntity<>(CommonResponse
                .<List<CampaignResponseDto>>builder("success : load campaign name list")
                .data(data)
                .build(), HttpStatus.OK);
    }

    @DeleteMapping("/deleteCampaign")
    public ResponseEntity<CommonResponse<String>> deleteCampaign(@RequestParam("campaignId") Long campaignId,
                                                                 BindingResult bindingResult) throws BindException {
        if(bindingResult.hasErrors()){
            throw new BindException(bindingResult);
        }
        campaignService.deleteCampaign(campaignId);
        return new ResponseEntity<>(CommonResponse.<String>builder("success delete").data("good").build(),HttpStatus.OK);
    }
}
