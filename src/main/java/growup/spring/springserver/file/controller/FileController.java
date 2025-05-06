package growup.spring.springserver.file.controller;

import growup.spring.springserver.file.FileType;
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

import java.util.List;

@RestController
@RequestMapping("/api/file")
@AllArgsConstructor
public class FileController {

    private final FileService fileService;

    // 타입별 파일 내역 보기
    // 프론트에서 파일타입이 넘어옴
    @GetMapping("/history")
    public ResponseEntity<CommonResponse<List<FileResDto>>> getFileHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam FileType fileType) {

        List<FileResDto> fileHistory = fileService.getFileHistory(fileType, userDetails.getUsername());

        return new ResponseEntity<>(CommonResponse
                .<List<FileResDto>>builder("success :getFileHistory")
                .data(fileHistory)
                .build(), HttpStatus.OK);
    }
}
