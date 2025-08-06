package growup.spring.springserver.exclusionKeyword.controller;

import growup.spring.springserver.exception.exclusionKeyword.ExclusionKeyNotFound;
import growup.spring.springserver.exception.global.RequestException;
import growup.spring.springserver.exclusionKeyword.dto.ExclusionKeywordRequestDto;
import growup.spring.springserver.exclusionKeyword.dto.ExclusionKeywordResponseDto;
import growup.spring.springserver.exclusionKeyword.service.ExclusionKeywordService;
import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.keyword.dto.KeywordResponseDto;
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

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/api/exclusionKeyword")
@RestController
@Slf4j
public class ExclusionKeywordController {
    @Autowired
    private ExclusionKeywordService exclusionKeywordService;

    @PostMapping("/addExclusionKeyword")
    public ResponseEntity<CommonResponse<?>> addExclusionKeyword(@Valid @RequestBody ExclusionKeywordRequestDto exclusionKeywordRequestDto,
                                                                 BindingResult bindingResult,
                                                                 @AuthenticationPrincipal UserDetails userDetails) throws BindException {
        log.info("start addExclusionKeyword");
        if (bindingResult.hasErrors()) {
            log.info("bindExceptions is throw");
            throw new RequestException();
        }
        final ExclusionKeywordResponseDto result = exclusionKeywordService.addExclusionKeyword(exclusionKeywordRequestDto,userDetails.getUsername());
        log.info("end addExclusionKeyword");
        return new ResponseEntity<>(CommonResponse
                .<ExclusionKeywordResponseDto>builder("success : add exclusionKeyword")
                .data(result)
                .build(), HttpStatus.OK);
    }

    @DeleteMapping("/remove")
    public ResponseEntity<CommonResponse<?>> removeExclusionKeyword(@Valid @RequestBody ExclusionKeywordRequestDto exclusionKeywordRequestDto,
                                                                    BindingResult bindingResult,
                                                                    @AuthenticationPrincipal UserDetails userDetails) throws BindException {
        log.info("start removeExclusionKeyword");
        if (bindingResult.hasErrors()) {
            log.info("bindExceptions is throw");
            throw new RequestException();
        }
        final boolean result = exclusionKeywordService.deleteExclusionKeyword(exclusionKeywordRequestDto,userDetails.getUsername());
        return new ResponseEntity<>(CommonResponse
                .<Boolean>builder("success : remove")
                .data(result)
                .build(), HttpStatus.OK);
    }

    @GetMapping("/getExclusionKeywords")
    public ResponseEntity<CommonResponse<?>> getExclusionKeywords(@RequestParam(value = "campaignId") Long campaignId){
        List<ExclusionKeywordResponseDto> result;
        try {
            result = exclusionKeywordService.getExclusionKeywords(campaignId);

        }catch (ExclusionKeyNotFound e){
            result = new ArrayList<>();
        }
        return new ResponseEntity<>(CommonResponse
                .<List<ExclusionKeywordResponseDto>>builder("success : getExclusionKeywords")
                .data(result)
                .build(), HttpStatus.OK);
    }
}
