package growup.spring.springserver.memo.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class MemoResponseDto {
    private Long id;
    private String contents;
    private String date;
}
