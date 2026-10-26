package growup.spring.springserver.global.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

@Getter
@Schema(description = "Error Response")
public class ErrorResponseDto {
    @Schema(description = "HTTP 상태 코드")
    private final String status;

    @Schema(description = "상태 메시지")
    private final String statusMessage;

    @Schema(description = "에러 메시지")
    private final String errorMessage;

    @Builder
    private ErrorResponseDto(HttpStatus status, String errorMessage) {
        this.status = String.valueOf(status.value());
        this.statusMessage = status.getReasonPhrase();
        this.errorMessage = errorMessage;
    }

    public static ResponseEntity<ErrorResponseDto> of(HttpStatus status, String errorMessage) {
        return new ResponseEntity<>(ErrorResponseDto.builder()
                .status(status)
                .errorMessage(errorMessage)
                .build(), status);
    }

    // Security filter용 메서드
    public static void writeToResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .status(status)
                .errorMessage(message)
                .build();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), errorResponse);
    }
}