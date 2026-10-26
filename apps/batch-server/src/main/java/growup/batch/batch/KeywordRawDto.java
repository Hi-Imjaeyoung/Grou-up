package growup.batch.batch;

import java.time.LocalDate;

public record KeywordRawDto(Long id, Long campaign_id, Double ad_cost, Double ad_sale, LocalDate date) {
}
