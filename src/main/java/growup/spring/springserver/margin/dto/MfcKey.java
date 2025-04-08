package growup.spring.springserver.margin.dto;

import growup.spring.springserver.marginforcampaign.support.MarginType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MfcKey {
    private String productName;
    private MarginType type;
}