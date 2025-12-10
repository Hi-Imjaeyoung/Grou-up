package growup.spring.springserver.marginforcampaign.controller;

import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.campaign.CampaignNotFoundException;
import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.global.exception.ErrorCode;
import growup.spring.springserver.global.exception.GrouException;
import growup.spring.springserver.marginforcampaign.service.ExcelService;
import growup.spring.springserver.marginforcampaign.service.MarginForCampaignService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@AllArgsConstructor
public class ExcelDownUpLoadController {

    private final ExcelService excelService;
    private final CampaignService campaignService;
    private final MarginForCampaignService marginForCampaignService;
    @GetMapping("/downloadExcel")
    public ResponseEntity<?> downloadExcel(@AuthenticationPrincipal UserDetails userDetails) {
        try (Workbook workbook = excelService.createUsersExcel(userDetails.getUsername());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out); // 엑셀 데이터를 메모리에 씁니다.
            ByteArrayResource resource = new ByteArrayResource(out.toByteArray());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"new-users-list.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);

        } catch (CampaignNotFoundException e) {
            log.warn("엑셀 다운로드 실패 (캠페인 없음): {}", userDetails.getUsername());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("다운로드할 캠페인이 존재하지 않습니다.");
        } catch (IOException e) {
            log.error("엑셀 파일 생성 IO 오류", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("엑셀 파일을 생성하는 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("엑셀 다운로드 중 알 수 없는 런타임 오류 발생", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.");
        }
    }
    @DeleteMapping("/upload")
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
                        .build(), HttpStatus.BAD_REQUEST);
            }
            if(e.getErrorCode().equals(ErrorCode.CAMPAIGN_NOT_FOUND)){
                return new ResponseEntity<>(CommonResponse.<String>builder("post fail")
                        .data("캠패인 ID가 잘못되었습니다. 액셀을 다시 다운로드해주세요.")
                        .build(), HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(CommonResponse.<String>builder("another error occur")
                    .data(e.getErrorCode().getMessage())
                    .build(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error(e.toString());
            return new ResponseEntity<>(CommonResponse.<String>builder("post fail")
                    .data("액셀 등록 실패")
                    .build(), HttpStatus.BAD_REQUEST);
        }
    }
}
