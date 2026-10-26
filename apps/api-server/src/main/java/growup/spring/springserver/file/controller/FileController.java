package growup.spring.springserver.file.controller;

import growup.spring.springserver.file.dto.FileResDto;
import growup.spring.springserver.file.service.FileService;
import growup.spring.springserver.global.common.CommonResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@AllArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping("/getHistory")
    public ResponseEntity<CommonResponse<FileResDto>> getFileHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("startDate") LocalDate start,
            @RequestParam("endDate") LocalDate end) {

        FileResDto fileHistory = fileService.getFileHistory(userDetails.getUsername(), start, end);

        return new ResponseEntity<>(CommonResponse
                .<FileResDto>builder("success :getFileHistory")
                .data(fileHistory)
                .build(), HttpStatus.OK);
    }

    @DeleteMapping("/deleteNetSalesFile")
    public ResponseEntity<CommonResponse<List<Map<String,Integer>>>> deleteCampaign(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("id") Long id
    ) {

        List<Map<String, Integer>> maps = fileService.deleteNetSalesFile(userDetails.getUsername(), id);

        return new ResponseEntity<>(CommonResponse
                .<List<Map<String,Integer>>>builder("success : deleteNetSalesReport")
                .data(maps)
                .build(), HttpStatus.OK);
    }
}
