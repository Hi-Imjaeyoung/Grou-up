package growup.spring.springserver.memo.controller;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.global.RequestException;
import growup.spring.springserver.exception.memo.MemoNotFoundException;
import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.global.dto.req.DateRangeRequest;
import growup.spring.springserver.memo.*;
import growup.spring.springserver.memo.domain.Memo;
import growup.spring.springserver.memo.dto.MemoRequestDto;
import growup.spring.springserver.memo.dto.MemoResponseDto;
import growup.spring.springserver.memo.dto.MemoUpdateRequestDto;
import growup.spring.springserver.memo.service.MemoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/memo")
public class MemoController {
    @Autowired
    private MemoService memoService;
    @Autowired
    private CampaignService campaignService;

    @PostMapping("/post")
    public ResponseEntity<CommonResponse<MemoResponseDto>> makeMemo(@Valid@RequestBody MemoRequestDto memoRequestDto,
                                                                    BindingResult bindingResult,
                                                                    @AuthenticationPrincipal UserDetails userDetails) throws BindException {
        if (bindingResult.hasErrors()) {
            log.info("bindExceptions is throw");
            throw new RequestException();
        }
        Campaign campaign = campaignService.getMyCampaign(memoRequestDto.getCampaignId(),userDetails.getUsername());
        Memo result = memoService.makeMemo(campaign,memoRequestDto);

        return new ResponseEntity<>(CommonResponse.<MemoResponseDto>builder("post success")
                .data(MemoTypeChanger.entityToDto(result))
                .build(), HttpStatus.OK);
    }

    @GetMapping("/getMemoAboutCampaign")
    public ResponseEntity<CommonResponse<List<MemoResponseDto>>> getMemoAboutCampaign(@RequestParam("campaignId") Long campaignId,
                                                                                      @AuthenticationPrincipal UserDetails userDetails){
        Campaign campaign = campaignService.getMyCampaign(campaignId, userDetails.getUsername());
        List<Memo> result = memoService.getMemoAboutCampaign(campaign);

        return new ResponseEntity<>(CommonResponse.<List<MemoResponseDto>>builder("get success")
                .data(result.stream().map(MemoTypeChanger::entityToDto).toList())
                .build(),HttpStatus.OK);
    }

    @PatchMapping("/update")
    public ResponseEntity<CommonResponse<MemoResponseDto>> updateMemo(@Valid @RequestBody MemoUpdateRequestDto memoRequestDto,
                                                                      BindingResult bindingResult) throws BindException {
        if(bindingResult.hasErrors()){
            throw new RequestException();
        }
        Memo result = memoService.updateMemo(memoRequestDto);
        return new ResponseEntity<>(CommonResponse.<MemoResponseDto>builder("update success")
                .data(MemoTypeChanger.entityToDto(result))
                .build(),HttpStatus.OK);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<CommonResponse<String>> deleteMemo(@RequestParam("memoId") Long memoId){
        String result;
        if(memoService.deleteMemo(memoId) == 1){
            result = "삭제 성공";
        }else{
            throw new MemoNotFoundException();
        }
        return new ResponseEntity<>(CommonResponse.<String>builder("delete success")
                .data(result)
                .build(),HttpStatus.OK);
    }

    @GetMapping("/getMemoByDate")
    public ResponseEntity<CommonResponse<Map<String,List<String>>>> getMemoByDate(@Valid @ModelAttribute DateRangeRequest dateRangeRequest,
                                                                                  @RequestParam("campaignId") Long campaignId){
        final Map<String,List<String>> map = memoService.getMemoByDateAndCampaign(dateRangeRequest.getStart(),dateRangeRequest.getEnd(),campaignId);
        return new ResponseEntity<>(CommonResponse.<Map<String,List<String>>>builder("get success")
                .data(map)
                .build(),HttpStatus.OK);
    }
}
