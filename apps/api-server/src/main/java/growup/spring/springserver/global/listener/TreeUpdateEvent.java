package growup.spring.springserver.global.listener;

import com.querydsl.core.Tuple;
import growup.spring.springserver.global.cache.AllCampaignTypeData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class TreeUpdateEvent {
    private final String email;
    private final List<Tuple> preData;
}
