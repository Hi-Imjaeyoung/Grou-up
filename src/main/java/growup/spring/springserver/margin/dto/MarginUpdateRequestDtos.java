package growup.spring.springserver.margin.dto;

import growup.spring.springserver.marginforcampaign.dto.MfcDto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MarginUpdateRequestDtos {
    @NotNull(message = "캠페인 ID를 입력해주세요.")
    private Long campaignId;
    @Size(min = 1, message = "1개 이상 수정해주세요.")
    private List<MarginUpdateRequestDto> data;
}

