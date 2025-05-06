package growup.spring.springserver.login.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(title = "MEM_RES_02 : 내 이메일 등급 DTO")
public record LoginDataResDto(

    @Schema(description = "이메일", example = "송보석")
    String name


) {}