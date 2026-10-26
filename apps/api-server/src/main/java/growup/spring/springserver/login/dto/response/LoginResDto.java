package growup.spring.springserver.login.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(title = "MEM_RES_01 : 로그인 응답 DTO")
public record LoginResDto(

    @Schema(description = "이메일", example = "test@test.com")
    String email,

    @Schema(description = "이름", example = "사용자")
    String name,

    @Schema(description = "토큰", example = "evv44v3940vi3094v3049uv9823vou2eoicjewoicjwec908wec89w")
    String accessToken
) {}