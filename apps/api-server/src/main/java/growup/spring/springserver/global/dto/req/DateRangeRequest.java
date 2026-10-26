package growup.spring.springserver.global.dto.req;


import growup.spring.springserver.global.annotaion.ValidDateRange;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@ValidDateRange(
        startField = "start",   // DTO 속성 이름
        endField   = "end",
        message    = "시작일자가 종료일자보다 클 수 없습니다."
)
public class DateRangeRequest {

    @NotNull(message = "startDate는 필수입니다.")
    private LocalDate start;

    @NotNull(message = "endDate는 필수입니다.")
    private LocalDate end;
}
