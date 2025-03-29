package growup.spring.springserver.campaign.controller;

import growup.spring.springserver.campaign.dto.CampaignDeleteDto;
import growup.spring.springserver.campaign.dto.CampaignResponseDto;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.campaignoptiondetails.service.CampaignOptionDetailsService;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.execution.dto.ExecutionMarginResDto;
import growup.spring.springserver.execution.service.ExecutionService;
import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import growup.spring.springserver.keyword.service.KeywordService;
import growup.spring.springserver.margin.service.MarginService;
import growup.spring.springserver.memo.service.MemoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Value;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/api/campaign")
@RestController
public class CampaignController {
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private KeywordService keywordService;
    @Autowired
    private MarginService marginService;
    @Autowired
    private MemoService memoService;
    @Autowired
    private ExecutionService executionService;
    @Autowired
    private CampaignOptionDetailsService campaignOptionDetailsService;
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
    public ResponseEntity<CommonResponse<Integer>> deleteCampaign(@RequestBody @NotEmpty List<Long> campaignIds,
                                                                 BindingResult bindingResult) throws BindException {
        if(bindingResult.hasErrors() || campaignIds.isEmpty()){
            log.error(bindingResult.toString());
            throw new BindException(bindingResult);
        }
        int deletedCampaignNumber = campaignService.deleteCampaign(campaignIds);
        return new ResponseEntity<>(CommonResponse.<Integer>builder("success delete")
                .data(deletedCampaignNumber)
                .build(),HttpStatus.OK);
    }

    @DeleteMapping("/deleteCampaignData")
    public ResponseEntity<CommonResponse<Map>> deleteCampaignData(@RequestBody @Valid CampaignDeleteDto campaignDeleteDto,
                                                                      BindingResult bindingResult) throws BindException {
        if(bindingResult.hasErrors()){
            log.error(bindingResult.toString());
            throw new BindException(bindingResult);
        }
        if(campaignDeleteDto.getEnd().isBefore(campaignDeleteDto.getStart())) throw new GrouException(ErrorCode.INVALID_DATE_FORMAT);
        Map<String,Integer> result = new HashMap<>();
        result.put("keyword",keywordService.deleteKeywordByCampaignIdsAndDate(campaignDeleteDto.getCampaignIds(),campaignDeleteDto.getStart(),campaignDeleteDto.getEnd()));
        result.put("margin",marginService.deleteKeywordByCampaignIdsAndDate(campaignDeleteDto.getCampaignIds(),campaignDeleteDto.getStart(),campaignDeleteDto.getEnd()));
        List<Long> executionIds = new ArrayList<>();
        for(Long campaignId : campaignDeleteDto.getCampaignIds()){
            executionIds.addAll(executionService.getMyExecutionData(campaignId).stream().map(ExecutionMarginResDto::getExeId).toList());
        }
        result.put("campaignOptionDetail",campaignOptionDetailsService.deleteKeywordByExecutionIdsAndDate(executionIds,campaignDeleteDto.getStart(),campaignDeleteDto.getEnd()));
        result.put("memo",memoService.deleteKeywordByCampaignIdsAndDate(campaignDeleteDto.getCampaignIds(),campaignDeleteDto.getStart(),campaignDeleteDto.getEnd()));
        return new ResponseEntity<>(CommonResponse.<Map>builder("success delete")
                .data(result)
                .build(),HttpStatus.OK);
    }

}
