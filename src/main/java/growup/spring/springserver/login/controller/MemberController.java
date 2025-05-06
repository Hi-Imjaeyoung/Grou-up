package growup.spring.springserver.login.controller;

import growup.spring.springserver.global.common.CommonResponse;
import growup.spring.springserver.global.error.ErrorResponseDto;
import growup.spring.springserver.login.dto.request.LoginSignInReqDto;
import growup.spring.springserver.login.dto.request.LoginSignUpReqDto;
import growup.spring.springserver.login.dto.response.LoginDataResDto;
import growup.spring.springserver.login.dto.response.LoginResDto;
import growup.spring.springserver.login.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Tag(name = "멤버 관리", description = "멤버 관련 API")
@RestController
@Slf4j
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/test1")
    public String test() {
        return "test success";
    }

    @GetMapping("/Authtest")
    @PreAuthorize("hasRole('SILVER')")
    public String Authtest(@AuthenticationPrincipal UserDetails userDetails) {
        log.info(userDetails.getUsername());
        log.info(userDetails.getAuthorities().toString());
        // 추가 로직 수행
        return "ok";
    }

    @Operation(summary = "멤버 API Summary", description = "회원가입 API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "member successfully created"),
            @ApiResponse(responseCode = "404", description = "already member exist",
                    content = {@Content(schema = @Schema(implementation = ErrorResponseDto.class))}),
    })
    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<String>> signup(@Valid @RequestBody LoginSignUpReqDto loginCreateReqDto, BindingResult bindingResult) throws BindException {
        log.info("start signup");
        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }
        memberService.signup(loginCreateReqDto);

        return new ResponseEntity<>(CommonResponse
                .<String>builder("member successfully created")
                .data(loginCreateReqDto.email())
                .build(), HttpStatus.CREATED);
    }

    @Operation(summary = "멤버 API Summary", description = "로그인 API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "login success"),
            @ApiResponse(responseCode = "404", description = "account cannot be found",
                    content = {@Content(schema = @Schema(implementation = ErrorResponseDto.class))}),
    })
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResDto>> login(@Valid @RequestBody LoginSignInReqDto loginSignInReqDto, BindingResult bindingResult) throws BindException {
        log.info("start login check");
        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }
        LoginResDto loginresDto = memberService.login(loginSignInReqDto);

        return new ResponseEntity<>(CommonResponse
                .<LoginResDto>builder("login success")
                .data(loginresDto)
                .build(), HttpStatus.CREATED);
    }

    @GetMapping("/getMyName")
    public ResponseEntity<CommonResponse<LoginDataResDto>> getMyEmailAndRole(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("start getMyEmailAndRole");
        LoginDataResDto myEmailAndRole = memberService.getMyName(userDetails.getUsername());

        return new ResponseEntity<>(CommonResponse
                .<LoginDataResDto>builder("getMyEmailAndRole Success")
                .data(myEmailAndRole)
                .build(), HttpStatus.OK
        );
    }
}