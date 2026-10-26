package growup.spring.springserver.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@AllArgsConstructor
@Getter
@ToString
@Schema(description = "공통 응답 래퍼")
public class CommonResponse<T> implements Serializable {
    @Schema(description = "응답 메시지", example = "성공")
    private final String message;

    @Schema(description = "응답 데이터")
    private final T data;


    public static class ResponseBuilder<T> {

        private final String message;
        private T data;

        private ResponseBuilder(String message) {
            this.message = message;
        }

        public ResponseBuilder<T> data(T value) {
            data = value;
            return this;
        }

        public CommonResponse<T> build() {
            return new CommonResponse<>(this);
        }
    }

    public static <T> ResponseBuilder<T> builder(String message) {
        return new ResponseBuilder<>(message);
    }

    private CommonResponse(ResponseBuilder<T> responseBuilder) {
        message = responseBuilder.message;
        data = responseBuilder.data;
    }
}
