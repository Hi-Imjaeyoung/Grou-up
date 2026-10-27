package growup.spring.springserver.campaign.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignDeleteDto {
    @NotNull(message = "시작 시간 누락")
    LocalDate start;
    @NotNull(message = "끝 시간 누락")
    LocalDate end;
    @NotEmpty(message = "캠패인 id 누락")
    List<Long> campaignIds;

    // 부분 업데이트는 삭제 일수 T에 대해 O(T log N), 전체 재빌드는 O(N)이라 짧은 기간일수록 부분 업데이트가 유리하다.
    // TreeCpuBenchmarkTest(2026-10-26)에서 60일과 90일 사이에 비용이 역전되어 60일 이하만 부분 업데이트한다.
    private static final long PARTIAL_UPDATE_MAX_DAYS = 60;

    // true: 부분 업데이트, false: 전체 재빌드
    public boolean checkThreshold(){
        if(start.getYear() == end.getYear()){
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            return days <= PARTIAL_UPDATE_MAX_DAYS;
        }
        return false;
    }
}
