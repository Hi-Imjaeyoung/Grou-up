package growup.spring.springserver.marginforcampaign.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MfcRequestDtos {
    @NotNull(message = "캠페인 ID를 입력해주세요.")
    private Long campaignId;

    @Size(min = 1, message = "1개 이상 수정해주세요.")
    private List<MfcDto> data;
}