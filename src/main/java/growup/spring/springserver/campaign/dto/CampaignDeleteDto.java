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

    public boolean checkThreshold(){
        if(start.getYear() == end.getYear()){
            long daysBetween = ChronoUnit.DAYS.between(start, end);
            return daysBetween >= 40;
        }
        return false;
    }
}
