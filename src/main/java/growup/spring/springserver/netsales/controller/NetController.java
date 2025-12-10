package growup.spring.springserver.netsales.controller;

import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.global.dto.req.DateRangeRequest;
import growup.spring.springserver.netsales.dto.TotalSalesDto;
import growup.spring.springserver.netsales.service.NetSalesService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/api/net")
@RestController
@AllArgsConstructor
@Slf4j
@Validated

public class NetController {
    private final NetSalesService netSalesService;

    @GetMapping("/getMyTotalSales")
    public ResponseEntity<CommonResponse<TotalSalesDto>> getMyTotalSales(
            @Valid @ModelAttribute DateRangeRequest dateRangeReq,
            @AuthenticationPrincipal UserDetails userDetails) {
        TotalSalesDto myTotalSales = netSalesService.getMyTotalSales(dateRangeReq.getStart(), dateRangeReq.getEnd(), userDetails.getUsername());

        return new ResponseEntity<>(CommonResponse
                .<TotalSalesDto>builder("success : getMyTotalSales")
                .data(myTotalSales)
                .build(), HttpStatus.OK);

    }
}