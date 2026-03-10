package growup.spring.springserver.global.listener;

import growup.spring.springserver.global.cache.AllCampaignTypeData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@AllArgsConstructor
public class TreeUpdateEvent {
    private final String email;
    private final Map<LocalDate,AllCampaignTypeData> preData;
}
