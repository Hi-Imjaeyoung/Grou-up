package growup.spring.springserver.keyword.controller;

import growup.spring.springserver.exception.keyword.CampaignKeywordNotFoundException;
import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.global.dto.req.DateRangeRequest;
import growup.spring.springserver.keyword.dto.KeywordResponseDto;
import growup.spring.springserver.keyword.dto.KeywordTotalDataResDto;
import growup.spring.springserver.keyword.service.KeywordService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/api/keyword")
@RestController
@Slf4j
public class KeywordController {
    @Autowired
    private KeywordService keywordService;

    @GetMapping("/getKeywordsAboutCampaign")
    public ResponseEntity<CommonResponse<?>> getKeywordAboutCampaign(@Valid @ModelAttribute DateRangeRequest dateRangeReq,
                                                                     @RequestParam("campaignId") Long campaignId,
                                                                     @AuthenticationPrincipal UserDetails userDetails){
//        log.info("start getKeywordAboutCampaign target "+ userDetails.getUsername());
        List<KeywordResponseDto> result;
        try {
            result = keywordService.getKeywordsByCampaignId(dateRangeReq.getStart(),dateRangeReq.getEnd(),campaignId);
        }catch (CampaignKeywordNotFoundException e){
            result = new ArrayList<>();
        }
//        log.info("end getKeywordAboutCampaign target "+ userDetails.getUsername());
        return new ResponseEntity<>(CommonResponse
                .<List<KeywordResponseDto>>builder("success : get keywords")
                .data(result)
                .build(), HttpStatus.OK);
    }

    @GetMapping("/getCampaignStat")
    public ResponseEntity<CommonResponse<KeywordTotalDataResDto>> getCampaignStat(@Valid @ModelAttribute DateRangeRequest dateRangeReq,
                                                                                  @RequestParam("campaignId") Long campaignId){
        KeywordTotalDataResDto result = keywordService.getTotalData(dateRangeReq.getStart(),dateRangeReq.getEnd(),campaignId);
        return new ResponseEntity<>(CommonResponse
                .<KeywordTotalDataResDto>builder("success : get keywords")
                .data(result)
                .build(), HttpStatus.OK);
    }
}
