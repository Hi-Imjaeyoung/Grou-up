package growup.spring.springserver.login.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(title = "MEM_REQ_01 : 로그인 요청 DTO")
public record LoginSignInReqDto(
        @NotBlank(message = "사용자 이메일을 입력해주세요")
        @Email(message = "이메일 형식에 맞지 않습니다.")
        @Schema(description = "사용자 이메일", example = "testtest@gmail.com")
        String email,

        @NotBlank
        @Schema(description = "비밀번호", example = "test123!")
        @Pattern(regexp="^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[A-Za-z\\d]{8,20}$|^(?=.*\\d)(?=.*[a-z])(?=.*[!#$%’()*+,./:;?@\\^_{|}~-])[a-z\\d!#$%’()*+,./:;?@\\^_{|}~-]{8,20}$|^(?=.*\\d)(?=.*[A-Z])(?=.*[!#$%’()*+,./:;?@\\^_{|}~-])[A-Z\\d!#$%’()*+,./:;?@\\^_{|}~-]{8,20}$|^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!#$%’()*+,./:;?@\\^_{|}~-])[A-Za-z\\d!#$%’()*+,./:;?@\\^_{|}~-]{8,20}$|^(?=.*[a-z])(?=.*[A-Z])(?=.*[!#$%’()*+,./:;?@\\^_{|}~-])[A-Za-z!#$%’()*+,./:;?@\\^_{|}~-]{8,20}$",
                message = "비밀번호는 영어 대,소문자와 숫자, 특수기호 중 적어도 3가지가 포함된 9자 ~ 20자로 이루어져야 합니다.")
        String password
        ){}
