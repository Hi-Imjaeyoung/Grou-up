package growup.spring.springserver.marginforcampaignchangedbyperiod.dto;

import java.time.LocalDate;

public record MarginChangeSaveRequestDto(
        Long mfcId,
        LocalDate startDate,
        LocalDate endDate,
        Long salePrice,
        Long totalPrice,
        Long costPrice,
        Long returnPrice
) {}