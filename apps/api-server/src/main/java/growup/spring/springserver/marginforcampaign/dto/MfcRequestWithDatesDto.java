package growup.spring.springserver.marginforcampaign.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@SuperBuilder // 부모 클래스 ㅂ   ㅣㄹ더 포함
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString(callSuper = true)
public class MfcRequestWithDatesDto extends MfcRequestDtos {
    @NotNull(message = "시작 날짜를 입력해주세요.")
    private LocalDate startDate;

    @NotNull(message = "종료 날짜를 입력해주세요.")
    private LocalDate endDate;
}