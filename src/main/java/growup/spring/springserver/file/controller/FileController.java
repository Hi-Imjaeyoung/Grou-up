package growup.spring.springserver.file.controller;

import growup.spring.springserver.file.dto.FileResDto;
import growup.spring.springserver.file.service.FileService;
import growup.spring.springserver.global.common.CommonResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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
}
