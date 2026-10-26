package growup.spring.springserver.memo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class MemoUpdateRequestDto {
    @NotNull(message = "수정할 메모의 정보가 누락되었습니다.")
    Long memoId;
    @NotNull(message = "수정할 내용을 보내야합니다.")
    String contents;
}
