package growup.spring.springserver.marginforcampaignchangedbyperiod.controller;

import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.marginforcampaignchangedbyperiod.dto.MarginChangeSaveRequestDto;
import growup.spring.springserver.marginforcampaignchangedbyperiod.service.MarginForCampaignChangedByPeriodService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/marginforcampaignchangedbyperiod")
@Slf4j
@AllArgsConstructor
public class MarginForCampaignChangedByPeriodController {
    private final MarginForCampaignChangedByPeriodService marginForCampaignChangedByPeriodService;
    // 기간 별 마진, 반품 비용 일 단위로 저장
    @PatchMapping("/save")
    public ResponseEntity<CommonResponse<String>> marginUpdatesByPeriod(@Valid @RequestBody MarginChangeSaveRequestDto requestDto
                                                                        ) {

        marginForCampaignChangedByPeriodService.save(requestDto);

        return ResponseEntity.ok(CommonResponse
                .<String>builder("success: marginforcampaignchangedbyperiodsave")
                .data("marginforcampaignchangedbyperiod update successful")  // 성공 메시지 반환
                .build());
    }
}