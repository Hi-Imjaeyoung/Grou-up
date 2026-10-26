package growup.spring.springserver.memo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class MemoRequestDto {
    @NotNull(message = "메모할 캠패인 id를 보내야합니다.")
    private Long campaignId;
    private Long id;
    @NotNull(message = "메모할 내용을 보내야합니다.")
    private String contents;
    @NotNull(message = "메모가 작성된 날짜를 보내야합니다.")
    private LocalDate date;
}
