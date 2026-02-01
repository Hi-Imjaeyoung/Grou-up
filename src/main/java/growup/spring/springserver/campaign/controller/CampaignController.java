package growup.spring.springserver.campaign.controller;

import growup.spring.springserver.campaign.facade.CampaignDeleteFacade;
import growup.spring.springserver.campaign.facade.CampaignTotalDataFacade;
import growup.spring.springserver.campaign.service.CampaignAnalysisService;
import growup.spring.springserver.campaign.TypeChangeCampaign;
import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.dto.CampaignDeleteDto;
import growup.spring.springserver.campaign.dto.CampaignResponseDto;
import growup.spring.springserver.campaign.dto.TotalCampaignsData;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.campaignoptiondetails.service.CampaignOptionDetailsService;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.exception.global.RequestException;
import growup.spring.springserver.exclusionKeyword.service.ExclusionKeywordService;
import growup.spring.springserver.execution.dto.ExecutionMarginResDto;
import growup.spring.springserver.execution.service.ExecutionService;
import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.global.dto.req.DateRangeRequest;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import growup.spring.springserver.keyword.service.KeywordService;
import growup.spring.springserver.keywordBid.service.KeywordBidService;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.service.MemberService;
import growup.spring.springserver.margin.service.MarginService;
import growup.spring.springserver.memo.service.MemoService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@AllArgsConstructor
public class CampaignController {
    private final CampaignService campaignService;
    private final KeywordService keywordService;
    private final MarginService marginService;
    private final MemoService memoService;
    private final ExecutionService executionService;
    private final CampaignOptionDetailsService campaignOptionDetailsService;
    private final MemberService memberService;
    private final CampaignAnalysisService campaignAnalysisService;
    private final CampaignDeleteFacade campaignDeleteFacade;
    private final CampaignTotalDataFacade campaignTotalDataFacade;


    @GetMapping("/getMyCampaigns")
    public ResponseEntity<CommonResponse<List<CampaignResponseDto>>> getMyCampaigns(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Start getMyCampaigns API target is :" + userDetails.getUsername());
        Member member = memberService.getMemberByEmail(userDetails.getUsername());
        List<CampaignResponseDto> data;
        try {
            data = campaignService.getCampaignsByMember(member).stream().map(TypeChangeCampaign::entityToResponseDto).toList();
        } catch (CampaignNotFoundException campaignNotFoundException){
            data = new ArrayList<>();
        }
        log.info("End getMyCampaigns API target is :" + userDetails.getUsername());
        return new ResponseEntity<>(CommonResponse
                .<List<CampaignResponseDto>>builder("success : load campaign name list")
                .data(data)
                .build(), HttpStatus.OK);
    }

    @DeleteMapping("/deleteCampaign")
    public ResponseEntity<CommonResponse<Integer>> refactoringDeleteCampaign(@RequestBody List<Long> campaignIds,
                                                                  BindingResult bindingResult) throws BindException {
        if(bindingResult.hasErrors() || campaignIds.isEmpty()){
            log.error(bindingResult.toString());
            throw new RequestException();
        }
        int deletedCampaignNumber = campaignDeleteFacade.deleteCampaign(campaignIds);
        return new ResponseEntity<>(CommonResponse.<Integer>builder("success delete")
                .data(deletedCampaignNumber)
                .build(),HttpStatus.OK);
    }

    @DeleteMapping("/deleteCampaignData")
    public ResponseEntity<CommonResponse<Map>> deleteCampaignData(@RequestBody @Valid CampaignDeleteDto campaignDeleteDto,
                                                                      BindingResult bindingResult) throws BindException {
        if(bindingResult.hasErrors()){
            log.error(bindingResult.toString());
            throw new RequestException();
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
    @GetMapping("/totalAnalysisData")
    public ResponseEntity<CommonResponse<TotalCampaignsData>> campaignTotalAnalysis(@Valid @ModelAttribute DateRangeRequest dateRangeRequest,
                                      @AuthenticationPrincipal UserDetails userDetails){
        Member member = memberService.getMemberByEmail(userDetails.getUsername());
        List<Campaign> campaigns = campaignService.getCampaignsByMember(member);
        TotalCampaignsData totalCampaignsData = campaignAnalysisService.getMyAllCampaignsDataByDate(dateRangeRequest.getStart(),dateRangeRequest.getEnd(),campaigns);
        return new ResponseEntity<>(CommonResponse.<TotalCampaignsData>builder("success get")
                .data(totalCampaignsData)
                .build(),HttpStatus.OK);
    }

    @GetMapping("/totalAnalysisData2")
    public ResponseEntity<CommonResponse<TotalCampaignsData>> campaignTotalAnalysis2(@Valid @ModelAttribute DateRangeRequest dateRangeRequest,
                                                                                    @AuthenticationPrincipal UserDetails userDetails){
        TotalCampaignsData totalCampaignsData =
                campaignTotalDataFacade.getCampaignTotalDataByCache(userDetails.getUsername(), dateRangeRequest.getStart(),dateRangeRequest.getEnd());
        return new ResponseEntity<>(CommonResponse.<TotalCampaignsData>builder("success get")
                .data(totalCampaignsData)
                .build(),HttpStatus.OK);
    }

    @GetMapping("/totalAnalysisData3")
    public ResponseEntity<CommonResponse<TotalCampaignsData>> campaignTotalAnalysis3(@Valid @ModelAttribute DateRangeRequest dateRangeRequest,
                                                                                     @AuthenticationPrincipal UserDetails userDetails){
        TotalCampaignsData totalCampaignsData =
                campaignTotalDataFacade.getCampaignTotalData(userDetails.getUsername(), dateRangeRequest.getStart(),dateRangeRequest.getEnd());
        return new ResponseEntity<>(CommonResponse.<TotalCampaignsData>builder("success get")
                .data(totalCampaignsData)
                .build(),HttpStatus.OK);
    }

    @GetMapping("/totalAnalysisData4")
    public ResponseEntity<CommonResponse<TotalCampaignsData>> campaignTotalAnalysis4(@Valid @ModelAttribute DateRangeRequest dateRangeRequest,
                                                                                     @AuthenticationPrincipal UserDetails userDetails){
        TotalCampaignsData totalCampaignsData =
                campaignTotalDataFacade.getCampaignTotalDataByLazyLoadingTree(userDetails.getUsername(), dateRangeRequest.getStart(),dateRangeRequest.getEnd());
        return new ResponseEntity<>(CommonResponse.<TotalCampaignsData>builder("success get")
                .data(totalCampaignsData)
                .build(),HttpStatus.OK);
    }


    @GetMapping("/cacheRate")
    public Map<String, Object> getStats() {
        Map<String, Long> stats = campaignTotalDataFacade.getCacheHitRate();
        // 보기 좋게 합쳐서 리턴
        return new HashMap<>(stats);
    }

    @PostMapping("/cacheReset")
    public void reset() {
        campaignTotalDataFacade.resetCacheStats();
    }
}
