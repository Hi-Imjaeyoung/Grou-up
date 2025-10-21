package growup.spring.springserver.marginforcampaign.controller;

import growup.spring.springserver.campaign.dto.CampaignIdAndNameForExcelDownload;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import growup.spring.springserver.marginforcampaign.service.ExcelService;
import growup.spring.springserver.marginforcampaign.service.MarginForCampaignService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marginforcam")
@AllArgsConstructor
public class ExcelDownUpLoadController {

    private final ExcelService excelService;
    private final CampaignService campaignService;
    private final MarginForCampaignService marginForCampaignService;
    @GetMapping("/downloadExcel")
    public ResponseEntity<?> downloadExcel(@AuthenticationPrincipal UserDetails userDetails) {

        try {
            List<CampaignIdAndNameForExcelDownload> campaignList =
                    campaignService.getCampaignsByEmail(userDetails.getUsername()).stream()
                            .map(campaign -> new CampaignIdAndNameForExcelDownload(campaign.getCampaignId(), campaign.getCamCampaignName()))
                            .toList();
            Workbook workbook = excelService.createUsersExcel(campaignList);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();
            ByteArrayResource resource = new ByteArrayResource(out.toByteArray());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"new-users-list.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);
        } catch (CampaignNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("다운로드할 캠페인이 존재하지 않습니다.");
        } catch (IOException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("엑셀 파일을 생성하는 중 오류가 발생했습니다.");
        }
    }
    @PostMapping("/upload")
    public ResponseEntity<CommonResponse<String>> uploadUsersExcel(@RequestParam("file") MultipartFile file,
                                                                   @AuthenticationPrincipal UserDetails userDetails) {
        if (file.isEmpty()) {
            return new ResponseEntity<>(CommonResponse.<String>builder("post fail")
                    .data("빈 파일입니다.")
                    .build(), HttpStatus.BAD_REQUEST);
        }
        try {
            Map<String,Integer> result = excelService.processUploadedExcel(file,userDetails.getUsername());
            return new ResponseEntity<>(CommonResponse.<String>builder("post success")
                    .data(result.toString())
                    .build(), HttpStatus.OK);
        } catch (GrouException e){
            if(e.getErrorCode().equals(ErrorCode.FILE_INVALID_DATA_FORM)){
                return new ResponseEntity<>(CommonResponse.<String>builder("post fail")
                        .data(e.getErrorCode().getMessage())
                        .build(), HttpStatus.OK);
            }
            return new ResponseEntity<>(CommonResponse.<String>builder("another error occur")
                    .data(e.getErrorCode().getMessage())
                    .build(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(CommonResponse.<String>builder("post fail")
                    .data("액셀 등록 실패")
                    .build(), HttpStatus.BAD_REQUEST);
        }
    }
}
