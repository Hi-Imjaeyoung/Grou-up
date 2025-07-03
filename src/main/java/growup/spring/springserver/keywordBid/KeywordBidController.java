package growup.spring.springserver.keywordBid;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.global.dto.req.DateRangeRequest;
import growup.spring.springserver.keyword.dto.KeywordResponseDto;
import growup.spring.springserver.keyword.service.KeywordService;
import growup.spring.springserver.keywordBid.dto.KeywordBidRequestDtos;
import growup.spring.springserver.keywordBid.dto.KeywordBidResponseDto;
import growup.spring.springserver.keywordBid.service.KeywordBidService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bid")
public class KeywordBidController {

    @Autowired
    private KeywordBidService keywordBidService;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private KeywordService keywordService;

    @PostMapping("/adds")
    public ResponseEntity<CommonResponse<KeywordBidResponseDto>> addKeywordBid(@AuthenticationPrincipal UserDetails userDetails,
                                                           @RequestBody @Valid KeywordBidRequestDtos keywordBidRequestDtos){
        Campaign campaign = campaignService.getMyCampaign(keywordBidRequestDtos.getCampaignId(), userDetails.getUsername());
        final KeywordBidResponseDto result = keywordBidService.addKeywordBids(keywordBidRequestDtos,campaign);
        return new ResponseEntity<>(CommonResponse
                .<KeywordBidResponseDto>builder("add bid data")
                .data(result)
                .build(), HttpStatus.OK);
    }

    @GetMapping("/gets")
    public ResponseEntity<CommonResponse<List<KeywordResponseDto>>> getKeywordBids(@Valid @ModelAttribute DateRangeRequest dateRangeReq,
                                                                     @RequestParam("campaignId") Long campaignId,
                                                                     @AuthenticationPrincipal UserDetails userDetails){
        KeywordBidResponseDto result = keywordBidService.getKeywordBids(campaignId);
        List<KeywordResponseDto> data = keywordService.getKeywordsByDateAndCampaignIdAndKeys(dateRangeReq.getStart(),dateRangeReq.getEnd(),campaignId,result.getResponse());
        return new ResponseEntity<>(CommonResponse
                .<List<KeywordResponseDto>>builder("get bids")
                .data(data)
                .build(), HttpStatus.OK);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<CommonResponse<KeywordBidResponseDto>> deleteKeywordBid(@RequestBody @Valid KeywordBidRequestDtos keywordBidRequestDtos){
        final KeywordBidResponseDto result = keywordBidService.deleteByCampaignIdAndKeyword(keywordBidRequestDtos);
        return new ResponseEntity<>(CommonResponse
                .<KeywordBidResponseDto>builder("delete bid data")
                .data(result)
                .build(), HttpStatus.OK);
    }

    @PatchMapping("/update")
    public ResponseEntity<CommonResponse<KeywordBidResponseDto>> updateKeywordBid(@RequestBody @Valid KeywordBidRequestDtos keywordBidRequestDtos){
        final KeywordBidResponseDto result = keywordBidService.updateKeywordBids(keywordBidRequestDtos);
        return new ResponseEntity<>(CommonResponse
                .<KeywordBidResponseDto>builder("update bid data")
                .data(result)
                .build(), HttpStatus.OK);
    }
}
